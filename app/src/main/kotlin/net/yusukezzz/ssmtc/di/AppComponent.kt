package net.yusukezzz.ssmtc.di

import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateService
import net.yusukezzz.ssmtc.ui.timeline.TimelineComponent
import net.yusukezzz.ssmtc.ui.timeline.TimelineModule
import net.yusukezzz.ssmtc.ui.timeline.dialogs.TimelineSettingDialog
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class
    ]
)
interface AppComponent {
    fun plus(timelineModule: TimelineModule): TimelineComponent
    fun inject(app: Application)
    fun inject(activity: AuthorizeActivity)
    fun inject(activity: StatusUpdateActivity)
    fun inject(dialog: TimelineSettingDialog)
    fun inject(service: StatusUpdateService)
}
