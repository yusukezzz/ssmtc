package net.yusukezzz.ssmtc.ui.status.update

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.NotificationCompat
import id.zelory.compressor.Compressor
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.util.getLongExtraOrNull
import java.io.File
import javax.inject.Inject

class StatusUpdateService: IntentService("StatusUpdateService") {
    companion object {
        const val ACTION_SUCCESS = "net.yusukezzz.ssmtc.screens.status.update.TWEET_SUCCESS"
        const val ACTION_FAILURE = "net.yusukezzz.ssmtc.screens.status.update.TWEET_FAILURE"

        const val ARG_STATUS_TEXT = "status_text"
        const val ARG_IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id"
        const val ARG_PHOTOS = "images"

        const val PHOTO_MAX_WIDTH = 1280
        const val PHOTO_MAX_HEIGHT = 960
        const val PHOTO_QUALITY = 85

        fun newIntent(context: Context,
                      status: String,
                      inReplyToStatusId: Long? = null,
                      photos: Array<String>? = null): Intent {
            return Intent(context, StatusUpdateService::class.java).apply {
                putExtra(ARG_STATUS_TEXT, status)
                inReplyToStatusId?.let { putExtra(ARG_IN_REPLY_TO_STATUS_ID, it) }
                photos?.let { putExtra(ARG_PHOTOS, it) }
            }
        }
    }

    private val compressor: Compressor by lazy {
        Compressor(this)
            .setMaxWidth(PHOTO_MAX_WIDTH)
            .setMaxHeight(PHOTO_MAX_HEIGHT)
            .setQuality(PHOTO_QUALITY)
    }

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: TwitterService

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    override fun onCreate() {
        super.onCreate()
        Application.component.inject(this)
    }

    override fun onHandleIntent(intent: Intent) {
        val status = intent.getStringExtra(ARG_STATUS_TEXT)
        val inReplyToStatusId = intent.getLongExtraOrNull(ARG_IN_REPLY_TO_STATUS_ID)
        val photos = intent.getStringArrayExtra(ARG_PHOTOS)

        val manager = NotificationManagerCompat.from(this)
        val builder = NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Tweet sending...")
        manager.notify(0, builder.build())

        try {
            val account = accountRepo.find(prefs.currentUserId)!!
            twitter.setTokens(account.credentials)

            val mediaIds = photos?.map {
                twitter.upload(compressor.compressImage(it)).media_id
            }

            twitter.tweet(status, inReplyToStatusId, mediaIds)
            sendSuccessBroadcast()
        } catch (e: Throwable) {
            println(e.message)
            e.printStackTrace()
            sendFailureBroadcast()
        } finally {
            manager.cancel(0)
            stopSelf()
        }
    }

    private fun sendSuccessBroadcast() = sendBroadcast(Intent(ACTION_SUCCESS))

    private fun sendFailureBroadcast() = sendBroadcast(Intent(ACTION_FAILURE))

    private fun Compressor.compressImage(path: String): File {
        val file = File(path)
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
