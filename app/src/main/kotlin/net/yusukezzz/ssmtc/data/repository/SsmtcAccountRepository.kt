package net.yusukezzz.ssmtc.data.repository

import android.accounts.Account
import android.accounts.AccountManager
import com.google.gson.Gson
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.model.User
import java.util.*

class SsmtcAccountRepository(
    private val am: AccountManager,
    private val gson: Gson,
    private val timelineRepository: TimelineRepository
) {
    companion object {
        const val ACCOUNT_DATA_ID = "account_data_id"
        const val ACCOUNT_DATA_USER = "account_data_user"
        const val ACCOUNT_DATA_LAST_TIMELINE_UUID = "account_data_last_timeline_uuid"
        const val ACCOUNT_TYPE = "net.yusukezzz.ssmtc.account"
        const val ACCOUNT_AUTH_TOKEN_TYPE = "net.yusukezzz.ssmtc.account.token"
    }

    fun findAll(): List<SsmtcAccount> {
        return am.getAccountsByType(ACCOUNT_TYPE).map {
            val cred = gson.fromJson(am.peekAuthToken(it, ACCOUNT_AUTH_TOKEN_TYPE), Credentials::class.java)
            val user = gson.fromJson(am.getUserData(it, ACCOUNT_DATA_USER), User::class.java)
            val uuid = UUID.fromString(am.getUserData(it, ACCOUNT_DATA_LAST_TIMELINE_UUID))
            val params = timelineRepository.findAll(user.id)
            SsmtcAccount(cred, user, params, uuid)
        }.sortedBy { it.user.id }
    }

    fun find(id: Long): SsmtcAccount? = findAll().find { it.user.id == id }

    fun add(ssmtcAccount: SsmtcAccount) {
        val account = Account(ssmtcAccount.user.screenName, ACCOUNT_TYPE)
        // Don't add UserData in this method, see http://stackoverflow.com/a/29776224/859190
        am.addAccountExplicitly(account, null, null)
        am.setAuthToken(account, ACCOUNT_AUTH_TOKEN_TYPE, gson.toJson(ssmtcAccount.credentials))

        save(account, ssmtcAccount)
    }

    fun update(ssmtcAccount: SsmtcAccount) {
        val account = Account(ssmtcAccount.user.screenName, ACCOUNT_TYPE)
        save(account, ssmtcAccount)
    }

    fun delete(ssmtcAccount: SsmtcAccount) {
        val account = Account(ssmtcAccount.user.screenName, ACCOUNT_TYPE)
        am.removeAccountExplicitly(account)
        timelineRepository.deleteAll(ssmtcAccount.user.id)
    }

    private fun save(account: Account, ssmtcAccount: SsmtcAccount) {
        am.setUserData(account, ACCOUNT_DATA_ID, ssmtcAccount.user.id.toString())
        am.setUserData(account, ACCOUNT_DATA_USER, gson.toJson(ssmtcAccount.user))
        am.setUserData(account, ACCOUNT_DATA_LAST_TIMELINE_UUID, ssmtcAccount.currentTimelineUuid.toString())

        timelineRepository.save(ssmtcAccount.user.id, ssmtcAccount.timelines)
    }
}
