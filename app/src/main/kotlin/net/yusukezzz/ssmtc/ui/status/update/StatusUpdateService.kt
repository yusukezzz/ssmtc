package net.yusukezzz.ssmtc.ui.status.update

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.NotificationCompat
import id.zelory.compressor.Compressor
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import java.io.File
import javax.inject.Inject

class StatusUpdateService: IntentService("StatusUpdateService") {
    companion object {
        const val ACTION_SUCCESS = "net.yusukezzz.ssmtc.screens.status.update.TWEET_SUCCESS"
        const val ACTION_FAILURE = "net.yusukezzz.ssmtc.screens.status.update.TWEET_FAILURE"

        const val ARG_STATUS_TEXT = "status_text"
        const val ARG_IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id"
        const val ARG_PHOTOS = "images"

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

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: Twitter

    @Inject
    lateinit var compressor: Compressor

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    override fun onCreate() {
        super.onCreate()
        Application.component.inject(this)
    }

    override fun onHandleIntent(intent: Intent) {
        val status = intent.getStringExtra(ARG_STATUS_TEXT)
        val inReplyToStatusId = if (intent.hasExtra(ARG_IN_REPLY_TO_STATUS_ID)) {
            intent.getLongExtra(ARG_IN_REPLY_TO_STATUS_ID, 0)
        } else {
            null
        }
        val photos = intent.getStringArrayExtra(ARG_PHOTOS)

        val manager = NotificationManagerCompat.from(this)
        val builder = NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Tweet sending...")
        manager.notify(0, builder.build())

        try {
            val account = accountRepo.find(prefs.currentUserId)!!
            val twitter = twitter.setTokens(account.credential.token, account.credential.tokenSecret)

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

    fun sendSuccessBroadcast() {
        val i = Intent(ACTION_SUCCESS)
        sendBroadcast(i)
    }

    fun sendFailureBroadcast() {
        val i = Intent(ACTION_FAILURE)
        sendBroadcast(i)
    }
}

fun Compressor.compressImage(path: String): File = this.compressToFile(File(path))
