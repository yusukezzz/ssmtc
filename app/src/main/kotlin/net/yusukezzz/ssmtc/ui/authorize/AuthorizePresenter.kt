package net.yusukezzz.ssmtc.ui.authorize

import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.data.repository.TimelineRepository
import net.yusukezzz.ssmtc.util.async
import net.yusukezzz.ssmtc.util.ui
import oauth.signpost.OAuth
import oauth.signpost.basic.DefaultOAuthConsumer
import oauth.signpost.basic.DefaultOAuthProvider

class AuthorizePresenter(val view: AuthorizeContract.View,
                         val prefs: Preferences,
                         val twitter: TwitterService,
                         val timelineRepo: TimelineRepository,
                         val accountRepo: SsmtcAccountRepository) : AuthorizeContract.Presenter {

    private val consumer = DefaultOAuthConsumer(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET)
    private val provider = DefaultOAuthProvider(
        "https://api.twitter.com/oauth/request_token",
        "https://api.twitter.com/oauth/access_token",
        "https://api.twitter.com/oauth/authorize?force_login=true"
    )

    override fun authorizeRequest() {
        ui {
            val url = async { provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND) }.await()
            view.showAuthorizeWeb(url)
        }
    }

    override fun authorize(pin: String) {
        ui {
            async {
                provider.retrieveAccessToken(consumer, pin)
                val cred = Credentials(consumer.token, consumer.tokenSecret)
                twitter.setTokens(cred)
                val user = twitter.verifyCredentials()
                prefs.currentUserId = user.id
                val timelines = timelineRepo.initialize(user.id)
                val ssmtcAccount = SsmtcAccount(cred, user, timelines, timelines.first().uuid)
                accountRepo.add(ssmtcAccount)
            }.await()
            view.authorized()
        }
    }

    override fun handleError(error: Throwable) {
        view.handleError(error)
    }
}

