package net.yusukezzz.ssmtc

import android.content.Context
import android.graphics.Bitmap.Config.RGB_565
import com.jakewharton.threetenabp.AndroidThreeTen
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
        installLeakCanary()

        startKovenant()
        initPicasso()
        initComponent()
    }

    override fun onTerminate() {
        stopKovenant()
        super.onTerminate()
    }

    open fun initPicasso() {
        Picasso.setSingletonInstance(Picasso.Builder(this).defaultBitmapConfig(RGB_565).build())
    }

    open fun initComponent() {
        AndroidThreeTen.init(this)
        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

    open protected fun installLeakCanary(): RefWatcher = RefWatcher.DISABLED
}
