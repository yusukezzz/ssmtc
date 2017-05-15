package net.yusukezzz.ssmtc.ui.authorize

import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.Credential
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.Twitter
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import oauth.signpost.OAuth
import oauth.signpost.basic.DefaultOAuthConsumer
import oauth.signpost.basic.DefaultOAuthProvider

class AuthorizePresenter(
    val view: AuthorizeContract.View,
    val prefs: Preferences,
    val twitter: Twitter): AuthorizeContract.Presenter {

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
            val home = TimelineParameter.home()
            val account = SsmtcAccount(it.token, it.tokenSecret, user, listOf(home), 0)
            prefs.saveAccount(account)
            prefs.currentUserId = user.id
            Pair(cred, user)
        } doneUi {
            view.authorized(it.first, it.second)
        }
    }

    override fun handleError(error: Throwable) {
        view.handleError(error)
    }
}

