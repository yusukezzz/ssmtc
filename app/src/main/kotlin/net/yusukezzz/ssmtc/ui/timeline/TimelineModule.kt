package net.yusukezzz.ssmtc.ui.timeline

import dagger.Module
import dagger.Provides
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.og.OGDiskCache
import net.yusukezzz.ssmtc.data.og.OpenGraphApi
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.di.PerActivity

@Module
class TimelineModule(val view: TimelineContract.View) {
    @Provides
    @PerActivity
    fun provideTimelineView(): TimelineContract.View = view

    @Provides
    @PerActivity
    fun provideOpenGraphService(cache: OGDiskCache, ogApi: OpenGraphApi): OpenGraphService =
        OpenGraphService(cache, ogApi, view.mainScope)

    @Provides
    @PerActivity
    fun provideTimelinePresenter(
        view: TimelineContract.View,
        twitter: TwitterService
    ): TimelineContract.Presenter = TimelinePresenter(view, twitter)
}
