package net.yusukezzz.ssmtc

import dagger.Component
import net.yusukezzz.ssmtc.di.AppComponent
import net.yusukezzz.ssmtc.di.TestAppModule
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateService
import net.yusukezzz.ssmtc.ui.timeline.TimelineActivity
import net.yusukezzz.ssmtc.ui.timeline.dialogs.TimelineSettingDialog
import javax.inject.Singleton

class TestApplication : Application() {
    val module: TestAppModule = TestAppModule(this)

    override fun initComponent() {
        // ignore for missing TZDB.dat
        //AndroidThreeTen.init(this)
        component = DaggerTestAppComponent.builder()
            .testAppModule(module)
            .build()
    }

    override fun installLeakCanary() {
        // do nothing
    }
}

@Singleton
@Component(modules = arrayOf(TestAppModule::class))
interface TestAppComponent : AppComponent {
    override fun inject(activity: TimelineActivity)
    override fun inject(activity: AuthorizeActivity)
    override fun inject(activity: StatusUpdateActivity)
    override fun inject(dialog: TimelineSettingDialog)
    override fun inject(service: StatusUpdateService)
}
