package net.yusukezzz.ssmtc

import android.graphics.Bitmap.Config.RGB_565
import com.deploygate.sdk.DeployGate
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.AppModule
import net.yusukezzz.ssmtc.di.DaggerAppComponent
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

open class Application : android.app.Application() {
    companion object {
        lateinit var component: AppComponent
    }

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
        startKovenant()
        Picasso.setSingletonInstance(Picasso.Builder(this).defaultBitmapConfig(RGB_565).build())
        initComponent()

        DeployGate.install(this)
    }

    override fun onTerminate() {
        stopKovenant()
        super.onTerminate()
    }

    open fun initComponent() {
        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}
