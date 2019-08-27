package net.yusukezzz.ssmtc

import com.squareup.leakcanary.RefWatcher
import dagger.Component
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.TestAppModule
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateService
import net.yusukezzz.ssmtc.ui.timeline.TimelineComponent
import net.yusukezzz.ssmtc.ui.timeline.TimelineModule
import net.yusukezzz.ssmtc.ui.timeline.dialogs.TimelineSettingDialog
import javax.inject.Singleton

class TestApplication : Application() {
    companion object {
        private var picassoInitialized = false
    }

    val module: TestAppModule = TestAppModule(this)

    override fun setupLeakCanary(): RefWatcher {
        // No leakcanary in unit tests
        return RefWatcher.DISABLED
    }

    override fun initPicasso() {
        if (!picassoInitialized) {
            // for robolectric test, picasso can set singleton instance only once
            picassoInitialized = true
            super.initPicasso()
        }
    }

    override fun initComponent() {
        // ignore for missing TZDB.dat
        // AndroidThreeTen.init(this)
        component = DaggerTestAppComponent.builder()
            .testAppModule(module)
            .build()
    }
}

@Singleton
@Component(modules = arrayOf(TestAppModule::class))
interface TestAppComponent : AppComponent {
    override fun plus(timelineModule: TimelineModule): TimelineComponent
    override fun inject(activity: AuthorizeActivity)
    override fun inject(activity: StatusUpdateActivity)
    override fun inject(dialog: TimelineSettingDialog)
    override fun inject(service: StatusUpdateService)
}
