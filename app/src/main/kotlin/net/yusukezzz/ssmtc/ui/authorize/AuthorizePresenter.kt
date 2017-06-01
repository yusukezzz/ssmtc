package net.yusukezzz.ssmtc.ui.authorize

import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.Credential
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.data.repository.TimelineRepository
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import oauth.signpost.OAuth
import oauth.signpost.basic.DefaultOAuthConsumer
import oauth.signpost.basic.DefaultOAuthProvider
import java.io.File

class AuthorizePresenter(val view: AuthorizeContract.View,
                         val prefs: Preferences,
                         val twitter: Twitter,
                         val timelineRepo: TimelineRepository,
                         val accountRepo: SsmtcAccountRepository) : AuthorizeContract.Presenter {

    private val consumer = DefaultOAuthConsumer(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET)
    private val provider = DefaultOAuthProvider(
        "https://api.twitter.com/oauth/request_token",
        "https://api.twitter.com/oauth/access_token",
        "https://api.twitter.com/oauth/authorize?force_login=true"
    )

    override fun authorizeRequest() {
        task {
            provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND)
        } doneUi {
            view.showAuthorizeWeb(it)
        }
    }

    override fun authorize(pin: String) {
        task {
            provider.retrieveAccessToken(consumer, pin)
            consumer
        } then {
            val cred = Credential(it.token, it.tokenSecret)
            val user = twitter.setTokens(it.token, it.tokenSecret).verifyCredentials()
            prefs.currentUserId = user.id
            val timelines = timelineRepo.initialize(user.id)
            val ssmtcAccount = SsmtcAccount(cred, user, timelines, timelines.first().uuid)
            accountRepo.add(ssmtcAccount)
            File("/data/user/0/net.yusukezzz.ssmtc/files/timelines").list().forEach { println(it) }
        } doneUi {
            view.authorized()
        }
    }

    override fun handleError(error: Throwable) {
        view.handleError(error)
    }
}

