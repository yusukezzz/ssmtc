package net.yusukezzz.ssmtc

import android.content.Context
import android.content.SharedPreferences
import com.github.gfx.util.encrypt.EncryptedSharedPreferences
import com.github.gfx.util.encrypt.Encryption
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.yusukezzz.ssmtc.data.Account
import net.yusukezzz.ssmtc.data.api.TimelineParameter

class Preferences(private val context: Context, private val gson: Gson) {
    companion object {
        const val KEY_ACCOUNTS_JSON = "accounts_json"
        const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    private val sharedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences(Encryption.getDefaultCipher(), context)
    }
    private val accountsType = object: TypeToken<List<Account>>() {}.type

    var currentUserId: Long
        get() = getLong(KEY_CURRENT_USER_ID)
        set(value) = put(KEY_CURRENT_USER_ID, value)

    var accounts: List<Account>
        get() {
            val json = getString(KEY_ACCOUNTS_JSON)
            return if (json.isEmpty()) {
                listOf()
            } else {
                gson.fromJson<List<Account>>(json, accountsType)
            }
        }
        private set(value) = put(KEY_ACCOUNTS_JSON, gson.toJson(value))

    fun getCurrentAccount(): Account? = getAccount(currentUserId)

    fun getAccount(userId: Long): Account? = accounts.find { it.user.id == userId }

    fun saveAccount(account: Account) {
        accounts = accounts.filterNot { it.user.id == account.user.id }.plus(account).sortedBy { it.user.id }
    }

    fun removeCurrentAccount() {
        accounts = accounts.minus(getCurrentAccount()!!)
        if (accounts.isEmpty()) {
            currentUserId = 0
        } else {
            currentUserId = accounts[0].user.id
        }
    }

    fun getCurrentTimeline(): TimelineParameter = getCurrentAccount()!!.timelines[currentTimelineIndex]

    var currentTimelineIndex: Int
        get() = getCurrentAccount()!!.lastTimelineIndex
        set(value) = getCurrentAccount()!!.run {
            lastTimelineIndex = value
            saveAccount(this)
        }

    fun addTimeline(param: TimelineParameter) {
        val account = getCurrentAccount()!!
        account.timelines = account.timelines.plus(param).sorted()
        saveAccount(account)
        currentTimelineIndex = account.timelines.indexOf(param)
    }

    fun updateCurrentTimeline(param: TimelineParameter) {
        val account = getCurrentAccount()!!
        val tmpList = account.timelines.toMutableList()
        val pos = tmpList.indexOf(getCurrentTimeline())
        tmpList[pos] = param
        account.timelines = tmpList.sorted()
        saveAccount(account)
        currentTimelineIndex = account.timelines.indexOf(param)
    }

    fun removeCurrentTimeline() {
        val account = getCurrentAccount()!!
        account.timelines = account.timelines.minus(getCurrentTimeline())
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
            else -> throw RuntimeException("unknown type: $value")
        }
        editor.apply()
    }
}
