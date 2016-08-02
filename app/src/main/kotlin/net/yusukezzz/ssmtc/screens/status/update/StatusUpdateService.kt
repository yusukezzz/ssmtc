package net.yusukezzz.ssmtc.screens.status.update

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.NotificationCompat
import id.zelory.compressor.Compressor
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.services.Twitter
import java.io.File

class StatusUpdateService: IntentService("StatusUpdateService") {
    companion object {
        val ACTION_SUCCESS = "net.yusukezzz.ssmtc.screens.status.update.TWEET_SUCCESS"
        val ACTION_FAILURE = "net.yusukezzz.ssmtc.screens.status.update.TWEET_FAILURE"
        val PHOTO_MAX_WIDTH: Float = 2048.0f
        val PHOTO_MAX_HEIGHT: Float = 1536.0f
        val PHOTO_QUALITY: Int = 85

        val ARG_STATUS_TEXT = "status_text"
        val ARG_IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id"
        val ARG_PHOTOS = "images"

        fun newIntent(context: Context,
                      status: String,
                      inReplyToStatusId: Long? = null,
                      photos: Array<String>? = null): Intent {
            return Intent(context, StatusUpdateService::class.java).apply {
                putExtra(ARG_STATUS_TEXT, status)
                if (null != inReplyToStatusId) {
                    putExtra(ARG_IN_REPLY_TO_STATUS_ID, inReplyToStatusId)
                }
                if (null != photos) {
                    putExtra(ARG_PHOTOS, photos)
                }
            }
        }
    }

    private val compressor by lazy {
        Compressor.Builder(this)
            .setMaxWidth(PHOTO_MAX_WIDTH)
            .setMaxHeight(PHOTO_MAX_HEIGHT)
            .setQuality(PHOTO_QUALITY)
            .setCompressFormat(Bitmap.CompressFormat.JPEG) // should respect original?
            .build()
    }

    override fun onHandleIntent(intent: Intent) {
        val status = intent.getStringExtra(ARG_STATUS_TEXT)
        val inReplyToStatusId = intent.getLongExtra(ARG_IN_REPLY_TO_STATUS_ID, 0)
        val photos = intent.getStringArrayExtra(ARG_PHOTOS)

        val manager = NotificationManagerCompat.from(this)
        val builder = NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Tweet sending...")
        manager.notify(0, builder.build())

        try {
            val account = Preferences(this).currentAccount!!
            val twitter = Twitter().setTokens(account.accessToken, account.secretToken)

            val mediaIds = photos?.map {
                twitter.upload(compressImage(it)).media_id
            }

            val res = twitter.tweet(status = status, mediaIds = mediaIds)
            println(res)
            sendSuccessBroadcast()
        } catch (e: Throwable) {
            println(e.message)
            e.printStackTrace()
            sendFailureBroadcast()
        } finally {
            stopSelf()
        }
    }

    fun sendSuccessBroadcast() {
        val i = Intent(ACTION_SUCCESS)
        sendBroadcast(i)
    }

    fun sendFailureBroadcast() {
        val i = Intent(ACTION_FAILURE)
        sendBroadcast(i)
    }

    /**
     * Convert original photo File to compressed photo File
     */
    private fun compressImage(path: String): File = compressor.compressToFile(File(path))
}
