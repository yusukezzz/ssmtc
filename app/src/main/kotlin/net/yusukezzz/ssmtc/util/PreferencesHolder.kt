package net.yusukezzz.ssmtc.util

import android.content.Context
import net.yusukezzz.ssmtc.Preferences

object PreferencesHolder {
    lateinit var prefs: Preferences

    fun init(context: Context) {
        prefs = Preferences(context)
    }
}
