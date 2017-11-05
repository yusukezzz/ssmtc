package net.yusukezzz.ssmtc.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import id.zelory.compressor.Compressor
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.data.repository.TimelineRepository
import org.mockito.Mockito
import javax.inject.Singleton

@Module
class TestAppModule(private val app: Application) {
    @Provides
    @Singleton
    fun provideApplication(): Application = app

    val mockGson = Gson()
    @Provides
    @Singleton
    fun provideGson(): Gson = mockGson

    val mockPrefs: Preferences = Mockito.mock(Preferences::class.java)
    @Provides
    @Singleton
    fun providePreferences(): Preferences = mockPrefs

    val mockTwitter: TwitterService = Mockito.mock(TwitterService::class.java)
    @Provides
    @Singleton
    fun provideTwitter(): TwitterService = mockTwitter

    val mockOGClient: OpenGraphService = Mockito.mock(OpenGraphService::class.java)
    @Provides
    @Singleton
    fun provideOpenGraphClient(): OpenGraphService = mockOGClient

    val mockCompressor: Compressor = Mockito.mock(Compressor::class.java)
    @Provides
    @Singleton
    fun provideCompressor(): Compressor = mockCompressor

    val mockAccountRepo: SsmtcAccountRepository = Mockito.mock(SsmtcAccountRepository::class.java)
    @Provides
    @Singleton
    fun provideSsmtcAccountRepository(): SsmtcAccountRepository = mockAccountRepo

    val mockTimelineRepo: TimelineRepository = Mockito.mock(TimelineRepository::class.java)
    @Provides
    @Singleton
    fun provideTimelineRepository(): TimelineRepository = mockTimelineRepo
}
