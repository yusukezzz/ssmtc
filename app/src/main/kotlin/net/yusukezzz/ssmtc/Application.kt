package net.yusukezzz.ssmtc

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap.Config.RGB_565
import android.os.Bundle
import com.deploygate.sdk.DeployGate
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.AppModule
import net.yusukezzz.ssmtc.di.DaggerAppComponent
import net.yusukezzz.ssmtc.ui.media.video.VideoPlayerActivity
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

        installLeakCanary()

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

    open protected fun installLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        val watcher = LeakCanary.refWatcher(this).build()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {}

            override fun onActivityResumed(activity: Activity?) {}

            override fun onActivityStarted(activity: Activity?) {}

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

            override fun onActivityStopped(activity: Activity?) {}

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

            override fun onActivityDestroyed(activity: Activity?) {
                // ignore VideoView context leak
                if (activity is VideoPlayerActivity) {
                    return
                }
                watcher.watch(activity)
            }
        })

        refWatcher = watcher
    }
}
