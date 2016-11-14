package net.yusukezzz.ssmtc

import com.deploygate.sdk.DeployGate
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

        DeployGate.install(this)
    }

    override fun onTerminate() {
        stopKovenant()
        super.onTerminate()
    }
}
