package net.yusukezzz.ssmtc

import com.facebook.stetho.Stetho
import net.danlew.android.joda.JodaTimeAndroid
import net.yusukezzz.ssmtc.services.Twitter
import net.yusukezzz.ssmtc.util.PreferencesHolder
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

class Application: android.app.Application() {
    val twitter by lazy { Twitter() }

    override fun onCreate() {
        super.onCreate()

        JodaTimeAndroid.init(applicationContext)
        PreferencesHolder.init(applicationContext)
        startKovenant()

        Stetho.initializeWithDefaults(this)
        //Picasso.with(this).isLoggingEnabled = true
        //Picasso.with(this).setIndicatorsEnabled(true)
    }

    override fun onTerminate() {
        stopKovenant()
        super.onTerminate()
    }
}
