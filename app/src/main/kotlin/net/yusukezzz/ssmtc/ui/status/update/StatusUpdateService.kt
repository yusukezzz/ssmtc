package net.yusukezzz.ssmtc.ui.status.update

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import id.zelory.compressor.Compressor
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.SlackService
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.util.getLongExtraOrNull
import net.yusukezzz.ssmtc.util.mediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class StatusUpdateService : IntentService("StatusUpdateService") {
    companion object {
        const val ACTION_SUCCESS = "net.yusukezzz.ssmtc.ui.status.update.TWEET_SUCCESS"
        const val ACTION_FAILURE = "net.yusukezzz.ssmtc.ui.status.update.TWEET_FAILURE"

        const val CHANNEL_ID = "status_update"
        const val CHANNEL_NAME = "tweets"
        const val CHANNEL_DESC = "Tweet sending ..."
        const val NOTIFICATION_ID = 0

        const val ARG_STATUS_TEXT = "status_text"
        const val ARG_IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id"
        const val ARG_MEDIAS = "medias"

        const val PHOTO_MAX_WIDTH = 1280
        const val PHOTO_MAX_HEIGHT = 960
        const val PHOTO_QUALITY = 85

        const val MB = 1024 * 1024
        const val MAX_VIDEO_SIZE = 512 * MB
        const val VIDEO_CHUNK_SIZE = 2 * MB
        const val MAX_POLLING_REQUESTS = 20

        fun newIntent(
            context: Context,
            status: String,
            inReplyToStatusId: Long? = null,
            medias: Array<String>? = null
        ): Intent {
            return Intent(context, StatusUpdateService::class.java).apply {
                putExtra(ARG_STATUS_TEXT, status)
                inReplyToStatusId?.let { putExtra(ARG_IN_REPLY_TO_STATUS_ID, it) }
                medias?.let { putExtra(ARG_MEDIAS, it) }
            }
        }
    }

    private val compressor: Compressor by lazy {
        Compressor(this)
            .setMaxWidth(PHOTO_MAX_WIDTH)
            .setMaxHeight(PHOTO_MAX_HEIGHT)
            .setQuality(PHOTO_QUALITY)
    }

    private val bcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(
            applicationContext
        )
    }

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: TwitterService

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    @Inject
    lateinit var slack: SlackService

    override fun onCreate() {
        super.onCreate()
        Application.component.inject(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val status = intent.getStringExtra(ARG_STATUS_TEXT)
        val inReplyToStatusId = intent.getLongExtraOrNull(ARG_IN_REPLY_TO_STATUS_ID)
        val medias = intent.getStringArrayExtra(ARG_MEDIAS)

        val manager = NotificationManagerCompat.from(this)
        showNotification(manager)

        try {
            val account = accountRepo.find(prefs.currentUserId)!!
            twitter.setTokens(account.credentials)

            val mediaIds = medias?.map { upload(it) }

            twitter.tweet(status, inReplyToStatusId, mediaIds)
            sendSuccessBroadcast()
        } catch (e: Throwable) {
            slack.sendMessage(e, BuildConfig.SLACK_CHANNEL)
            sendFailureBroadcast()
        } finally {
            manager.cancel(NOTIFICATION_ID)
        }
    }

    private fun showNotification(manager: NotificationManagerCompat) {
        val importance = NotificationManager.IMPORTANCE_LOW
        val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
        ch.description = CHANNEL_DESC
        ch.enableVibration(false)
        ch.enableLights(false)
        ch.setShowBadge(false)
        manager.createNotificationChannel(ch)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Tweet sending...")
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun upload(path: String): Long {
        val file = File(path)

        if (file.mediaType().type == "image") {
            return uploadImage(file)
        }

        if (file.mediaType().type == "video") {
            return uploadVideo(file)
        }

        throw RuntimeException("unsupported file: $path")
    }

    private fun uploadImage(file: File): Long {
        val beforeSize = file.length()
        val image = compressor.compressImage(file)
        val afterSize = image.length()
        println("[image] original    size: ${beforeSize / 1024}KB")
        println("[image] compressed  size: ${afterSize / 1024}KB")
        println("[image] compressed ratio: ${afterSize.toFloat() / beforeSize * 100}%")
        return twitter.upload(image.asRequestBody(image.mediaType())).media_id
    }

    private fun uploadVideo(file: File): Long {
        val type = file.mediaType()
        val totalBytes = file.length()
        if (totalBytes > MAX_VIDEO_SIZE) {
            throw RuntimeException("[video] file too large: max ${MAX_VIDEO_SIZE / MB} MBytes")
        }

        println("[video] init start")
        val mediaId = twitter.uploadInit(totalBytes).media_id

        val input = file.inputStream()
        var segmentIndex = 0
        while (true) {
            val data = ByteArray(VIDEO_CHUNK_SIZE)
            val bytesRead = input.read(data)
            if (bytesRead == -1) break
            val baInput = data.inputStream(0, bytesRead)
            val body = InputStreamBody(baInput, bytesRead.toLong(), type)
            twitter.uploadAppend(mediaId, segmentIndex, body)
            println("[video] append index=$segmentIndex")
            segmentIndex++
        }

        println("[video] finalize")
        twitter.uploadFinalize(mediaId).processing_info?.let {
            println(it)
            Thread.sleep(it.check_after_secs * 1000L)
        }

        for (i in 1..MAX_POLLING_REQUESTS) {
            println("[video] status polling ... $i")
            val info = twitter.uploadStatus(mediaId).processing_info!!
            println(info)
            if (info.state == "failed") {
                throw RuntimeException("[video] finalize failed: ${info.error}")
            }
            if (info.state == "succeeded") {
                println("[video] complete")
                return mediaId
            }

            // state = pending or in_progress
            Thread.sleep(info.check_after_secs * 1000L)
        }

        throw RuntimeException("[video] finalize failed: status polling max")
    }

    private fun sendSuccessBroadcast() = bcastManager.sendBroadcast(Intent(ACTION_SUCCESS))

    private fun sendFailureBroadcast() = bcastManager.sendBroadcast(Intent(ACTION_FAILURE))

    private fun Compressor.compressImage(file: File): File {
        val format = when (file.extension.toLowerCase()) {
            "jpg" -> Bitmap.CompressFormat.JPEG
            "jpeg" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG // try convert to jpeg
        }
        return this.setCompressFormat(format).compressToFile(file)
    }
}
