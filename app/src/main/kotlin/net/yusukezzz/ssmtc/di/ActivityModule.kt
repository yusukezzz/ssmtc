package net.yusukezzz.ssmtc.di

import dagger.android.ContributesAndroidInjector
import net.yusukezzz.ssmtc.ui.timeline.TimelineActivity
import net.yusukezzz.ssmtc.ui.timeline.TimelineModule

abstract class ActivityModule {
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    internal abstract fun contributeTimelineActivityInjector(): TimelineActivity
}