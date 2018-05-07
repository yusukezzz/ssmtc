package net.yusukezzz.ssmtc

import android.graphics.Bitmap.Config.RGB_565
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.data.SlackService
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.AppModule
import net.yusukezzz.ssmtc.di.DaggerAppComponent
import net.yusukezzz.ssmtc.util.async
import net.yusukezzz.ssmtc.util.prettyMarkdown
import net.yusukezzz.ssmtc.util.ui
import saschpe.android.customtabs.CustomTabsActivityLifecycleCallbacks
import java.io.File
import javax.inject.Inject

open class Application : android.app.Application() {
    companion object {
        lateinit var component: AppComponent
        private const val ERROR_LOG_FILENAME = "error.log"
    }

    @Inject
    lateinit var slack: SlackService

    private lateinit var savedUncaughtExceptionHandler: Thread.UncaughtExceptionHandler

    override fun onCreate() {
        super.onCreate()

        //setupLeakCanary()

        initPicasso()
        initComponent()

        registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())

        savedUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this@Application::handleUncaughtException)

        sendErrorLog()
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
        component.inject(this)
    }

    private fun handleUncaughtException(thread: Thread, e: Throwable) {
        try {
            File(applicationContext.filesDir, ERROR_LOG_FILENAME).writeText(e.prettyMarkdown())
        } finally {
            savedUncaughtExceptionHandler.uncaughtException(thread, e)
        }
    }

    private fun sendErrorLog() {
        val log = File(applicationContext.filesDir, ERROR_LOG_FILENAME)
        if (log.exists()) {
            ui {
                try {
                    async { slack.sendMessage(log.readText(), BuildConfig.SLACK_CHANNEL) }.await()
                    log.delete()
                } catch (e: Throwable) {
                    // do nothing
                    println(e)
                }
            }
        }
    }
}
