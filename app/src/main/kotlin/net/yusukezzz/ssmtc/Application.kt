package net.yusukezzz.ssmtc

import android.graphics.Bitmap.Config.RGB_565
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.AppModule
import net.yusukezzz.ssmtc.di.DaggerAppComponent
import saschpe.android.customtabs.CustomTabsActivityLifecycleCallbacks

open class Application : android.app.Application() {
    companion object {
        lateinit var component: AppComponent
    }

    override fun onCreate() {
        super.onCreate()

        //setupLeakCanary()

        initPicasso()
        initComponent()

        registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())
    }

    protected open fun setupLeakCanary(): RefWatcher {
        if (LeakCanary.isInAnalyzerProcess(this) || !BuildConfig.DEBUG) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return RefWatcher.DISABLED
        }
        return LeakCanary.install(this)
    }

    protected open fun initPicasso() {
        Picasso.setSingletonInstance(Picasso.Builder(this).defaultBitmapConfig(RGB_565).build())
    }

    protected open fun initComponent() {
        AndroidThreeTen.init(this)
        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}
