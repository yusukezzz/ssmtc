package net.yusukezzz.ssmtc

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class Preferences(private val context: Context, private val gson: Gson) {
    companion object {
        const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    var currentUserId: Long
        get() = getLong(KEY_CURRENT_USER_ID)
        set(value) = put(KEY_CURRENT_USER_ID, value)

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
