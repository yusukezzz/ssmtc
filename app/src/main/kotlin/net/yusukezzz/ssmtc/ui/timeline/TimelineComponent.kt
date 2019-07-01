package net.yusukezzz.ssmtc.ui.timeline

import dagger.Subcomponent
import net.yusukezzz.ssmtc.di.PerActivity

@PerActivity
@Subcomponent(modules = [TimelineModule::class])
interface TimelineComponent {
    fun inject(activity: TimelineActivity)
}
