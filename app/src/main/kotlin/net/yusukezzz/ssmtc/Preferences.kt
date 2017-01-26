package net.yusukezzz.ssmtc

import android.content.Context
import android.content.SharedPreferences
import com.github.gfx.util.encrypt.EncryptedSharedPreferences
import com.github.gfx.util.encrypt.Encryption
import com.google.gson.reflect.TypeToken
import net.yusukezzz.ssmtc.data.Account
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.util.gson.GsonHolder

open class Preferences(private val context: Context) {
    companion object {
        const val KEY_ACCOUNTS_JSON = "accounts_json"
        const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    private val sharedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences(Encryption.getDefaultCipher(), context)
    }
    private val accountsType = object: TypeToken<List<Account>>() {}.type

    val currentAccount: Account?
        get() = getAccount(currentUserId)

    var currentUserId: Long
        get() = getLong(KEY_CURRENT_USER_ID)
        set(value) = put(KEY_CURRENT_USER_ID, value)

    var accounts: List<Account>
        get() {
            val json = getString(KEY_ACCOUNTS_JSON)
            return if (json.isEmpty()) {
                listOf()
            } else {
                GsonHolder.gson.fromJson<List<Account>>(json, accountsType)
            }
        }
        private set(value) = put(KEY_ACCOUNTS_JSON, GsonHolder.gson.toJson(value))

    fun getAccount(userId: Long): Account? = accounts.find { it.user.id == userId }

    fun saveAccount(account: Account) {
        accounts = accounts.filterNot { it.user.id == account.user.id }.plus(account).sortedBy { it.user.id }
    }

    fun removeCurrentAccount() {
        accounts = accounts.minus(currentAccount!!)
        if (accounts.isEmpty()) {
            currentUserId = 0
        } else {
            currentUserId = accounts[0].user.id
        }
    }

    val currentTimeline: TimelineParameter
        get() = currentAccount!!.timelines[currentTimelineIndex]

    var currentTimelineIndex: Int
        get() = currentAccount!!.lastTimelineIndex
        set(value) = currentAccount!!.run {
            lastTimelineIndex = value
            saveAccount(this)
        }

    fun addTimeline(param: TimelineParameter) {
        val account = currentAccount!!
        account.timelines = account.timelines.plus(param).sorted()
        saveAccount(account)
        currentTimelineIndex = account.timelines.indexOf(param)
    }

    fun updateCurrentTimeline(param: TimelineParameter) {
        val account = currentAccount!!
        val tmpList = account.timelines.toMutableList()
        val pos = tmpList.indexOf(currentTimeline)
        tmpList[pos] = param
        account.timelines = tmpList.sorted()
        saveAccount(account)
        currentTimelineIndex = account.timelines.indexOf(param)
    }

    fun removeCurrentTimeline() {
        val account = currentAccount!!
        account.timelines = account.timelines.minus(currentTimeline)
        saveAccount(account)
        currentTimelineIndex = 0
    }

    private fun getString(key: String): String = sharedPrefs.getString(key, "")
    private fun getLong(key: String): Long = sharedPrefs.getLong(key, 0)
    private fun put(key: String, value: Any) {
        val editor = sharedPrefs.edit()
        when (value) {
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> editor.putStringSet(key, value as Set<String>)
            else -> throw RuntimeException("unknown type")
        }
        editor.apply()
    }
}
