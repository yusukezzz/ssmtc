package net.yusukezzz.ssmtc.ui.timeline

import dagger.Module
import dagger.Provides
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.di.PerActivity

@Module
class TimelineModule(val view: TimelineContract.View) {
    @Provides
    @PerActivity
    fun provideTimelineView(): TimelineContract.View = view

    @Provides
    @PerActivity
    fun provideTimelinePresenter(
        view: TimelineContract.View,
        twitter: TwitterService
    ): TimelineContract.Presenter = TimelinePresenter(view, twitter)
}
