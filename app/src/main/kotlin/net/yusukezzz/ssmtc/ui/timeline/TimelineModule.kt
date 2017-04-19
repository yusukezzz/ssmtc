package net.yusukezzz.ssmtc.ui.timeline

import dagger.Module
import dagger.Provides
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.di.ActivityScope

@Module
class TimelineModule(val view: TimelineContract.View) {
    @Provides
    @ActivityScope
    fun provideTimelineView(): TimelineContract.View = view

    @Provides
    @ActivityScope
    fun provideTimelinePresenter(view: TimelineContract.View,
                                 twitter: Twitter): TimelineContract.Presenter = TimelinePresenter(view, twitter)
}