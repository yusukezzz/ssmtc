package net.yusukezzz.ssmtc.ui.status.update

import android.app.IntentService
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
import net.yusukezzz.ssmtc.util.mimeType
import java.io.File
import javax.inject.Inject

class StatusUpdateService: IntentService("StatusUpdateService") {
    companion object {
        const val ACTION_SUCCESS = "net.yusukezzz.ssmtc.ui.status.update.TWEET_SUCCESS"
        const val ACTION_FAILURE = "net.yusukezzz.ssmtc.ui.status.update.TWEET_FAILURE"

        const val ARG_STATUS_TEXT = "status_text"
        const val ARG_IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id"
        const val ARG_MEDIAS = "medias"

        const val PHOTO_MAX_WIDTH = 1280
        const val PHOTO_MAX_HEIGHT = 960
        const val PHOTO_QUALITY = 85

        const val VIDEO_MAX_LENGTH = 140 // 2m20sec

        fun newIntent(context: Context,
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

    private val bcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(applicationContext) }

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

    override fun onHandleIntent(intent: Intent) {
        val status = intent.getStringExtra(ARG_STATUS_TEXT)
        val inReplyToStatusId = intent.getLongExtraOrNull(ARG_IN_REPLY_TO_STATUS_ID)
        val medias = intent.getStringArrayExtra(ARG_MEDIAS)

        val manager = NotificationManagerCompat.from(this)
        val builder = NotificationCompat.Builder(this, "ch-tweet")
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Tweet sending...")
        manager.notify(0, builder.build())

        try {
            val account = accountRepo.find(prefs.currentUserId)!!
            twitter.setTokens(account.credentials)

            val mediaIds = medias?.map {
                twitter.upload(compressor.compressImage(it)).media_id
            }

            twitter.tweet(status, inReplyToStatusId, mediaIds)
            sendSuccessBroadcast()
        } catch (e: Throwable) {
            slack.sendMessage(e, BuildConfig.SLACK_CHANNEL)
            sendFailureBroadcast()
        } finally {
            manager.cancel(0)
        }
    }

    private fun upload(path: String): Long {
        val file = File(path)
        when {
            file.mimeType().startsWith("image") -> uploadImage(file)
            file.mimeType().startsWith("video") -> {
            }
            else -> throw RuntimeException("unsupported file: $path")
        }
    }

    // TODO: resize image InputStream
    private fun uploadImage(file: File): Long = twitter.upload(compressor.compressImage(file)).media_id

    private fun uploadVideo(file: File): Long {
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
