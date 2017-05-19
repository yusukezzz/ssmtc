package net.yusukezzz.ssmtc.ui.authorize

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.authorize.*
import kotlinx.android.synthetic.main.base_layout.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.Credential
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.model.User
import net.yusukezzz.ssmtc.util.setView
import net.yusukezzz.ssmtc.util.toast
import javax.inject.Inject

class AuthorizeActivity : AppCompatActivity(), AuthorizeContract.View {
    companion object {
        const val ACCOUNT_DATA_ID = "account_data_id"
        const val ACCOUNT_DATA_USER = "account_data_user"
        const val ACCOUNT_TYPE = "net.yusukezzz.ssmtc.account"
        const val ACCOUNT_AUTH_TOKEN_TYPE = "net.yusukezzz.ssmtc.account.token"
    }

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: Twitter

    @Inject
    lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.inject(this)

        setContentView(R.layout.base_layout)
        main_contents.setView(R.layout.authorize)

        toolbar_title.text = "Authorization"
        val presenter = AuthorizePresenter(this, prefs, twitter)

        btn_authorize_request.setOnClickListener { presenter.authorizeRequest() }
        btn_authorize.setOnClickListener { presenter.authorize(edit_pin_code.text.toString()) }
    }

    override fun showAuthorizeWeb(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    override fun authorized(credential: Credential, user: User) {
        addAccount(credential, user)
        val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }

    override fun handleError(error: Throwable) {
        toast(error)
    }

    private fun addAccount(credential: Credential, user: User) {
        val am = AccountManager.get(this)
        val account = Account(user.screenName, ACCOUNT_TYPE)
        // Don't add UserData in this method, see http://stackoverflow.com/a/29776224/859190
        am.addAccountExplicitly(account, null, null)

        am.setUserData(account, ACCOUNT_DATA_ID, user.id.toString())
        am.setUserData(account, ACCOUNT_DATA_USER, gson.toJson(user))
        am.setAuthToken(account, ACCOUNT_AUTH_TOKEN_TYPE, gson.toJson(credential))
    }
}
