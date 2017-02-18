package net.yusukezzz.ssmtc

import android.graphics.Bitmap.Config.RGB_565
import com.deploygate.sdk.DeployGate
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.util.PreferencesHolder
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

class Application: android.app.Application() {
    val twitter by lazy { Twitter() }

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
        PreferencesHolder.init(this)
        startKovenant()
        Picasso.setSingletonInstance(Picasso.Builder(this).defaultBitmapConfig(RGB_565).build())

        DeployGate.install(this)
    }

    override fun onTerminate() {
        stopKovenant()
        super.onTerminate()
    }
}
