package net.yusukezzz.ssmtc

import android.graphics.Bitmap.Config.RGB_565
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import net.yusukezzz.ssmtc.data.SlackService
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.AppModule
import net.yusukezzz.ssmtc.di.DaggerAppComponent
import net.yusukezzz.ssmtc.util.prettyMarkdown
import saschpe.android.customtabs.CustomTabsActivityLifecycleCallbacks
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

open class Application : android.app.Application(), CoroutineScope {
    companion object {
        lateinit var component: AppComponent
        private const val ERROR_LOG_FILENAME = "error.log"
    }

    private val supervisor = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisor

    @Inject
    lateinit var slack: SlackService

    private lateinit var savedUncaughtExceptionHandler: Thread.UncaughtExceptionHandler

    override fun onCreate() {
        super.onCreate()

        // setupLeakCanary()

        initPicasso()
        initComponent()

        registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())

        savedUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()!!
        Thread.setDefaultUncaughtExceptionHandler(this@Application::handleUncaughtException)

        sendErrorLog()
    }

    protected open fun initPicasso() {
        Picasso.setSingletonInstance(Picasso.Builder(this).defaultBitmapConfig(RGB_565).build())
    }

    protected open fun initComponent() {
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
            launch {
                try {
                    withContext(Dispatchers.IO) {
                        val text = log.readText()
                        slack.sendMessage(text, BuildConfig.SLACK_CHANNEL)
                        log.delete()
                    }
                } catch (e: Throwable) {
                    // do nothing
                    println(e)
                }
            }
        }
    }
}
