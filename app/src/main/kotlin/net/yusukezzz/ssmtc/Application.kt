package net.yusukezzz.ssmtc

import android.content.Context
import android.graphics.Bitmap.Config.RGB_565
import com.deploygate.sdk.DeployGate
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.AndroidExcludedRefs
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.AppModule
import net.yusukezzz.ssmtc.di.DaggerAppComponent
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

open class Application : android.app.Application() {
    companion object {
        lateinit var component: AppComponent
        fun getRefWatcher(context: Context): RefWatcher = (context.applicationContext as Application).refWatcher
    }

    private lateinit var refWatcher: RefWatcher

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        refWatcher = installLeakCanary()

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
        AndroidThreeTen.init(this)
        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

    protected fun installLeakCanary(): RefWatcher {
        val excludes = AndroidExcludedRefs.createAppDefaults()
            .instanceField("android.media.MediaPlayer", "mSubtitleController")
            .build()
        return LeakCanary.refWatcher(this)
            .excludedRefs(excludes)
            .buildAndInstall()
    }
}
