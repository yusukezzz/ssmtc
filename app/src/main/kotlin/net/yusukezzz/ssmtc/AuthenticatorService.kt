package net.yusukezzz.ssmtc

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity

class AccountAuthenticatorService : Service() {
    private lateinit var authenticator: SsmtcAccountAuthenticator
    override fun onCreate() {
        super.onCreate()
        authenticator = SsmtcAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder = authenticator.iBinder

    internal class SsmtcAccountAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {
        override fun addAccount(response: AccountAuthenticatorResponse,
                                accountType: String?,
                                authTokenType: String?,
                                requiredFeatures: Array<String>?,
                                options: Bundle?): Bundle {
            val i = Intent(context, AuthorizeActivity::class.java)
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            return Bundle().apply { putParcelable(AccountManager.KEY_INTENT, i) }
        }

        override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle?): Bundle {
            val am = AccountManager.get(context)
            val token = am.peekAuthToken(account, authTokenType)
            if (token.isNullOrEmpty()) {
                return addAccount(response, null, null, null, null)
            }

            return Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
                putString(AccountManager.KEY_AUTHTOKEN, token)
            }
        }

        override fun getAuthTokenLabel(authTokenType: String): String = authTokenType

        override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle = defaultResponse()

        override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle = defaultResponse()

        override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle = defaultResponse()

        override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle = defaultResponse()

        private fun defaultResponse(): Bundle = Bundle().apply {
            putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
        }

    }
}
