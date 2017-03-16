package net.yusukezzz.ssmtc.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.og.OpenGraphClient
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

    val mockPrefs = Mockito.mock(Preferences::class.java)
    @Provides
    @Singleton
    fun providePreferences(): Preferences = mockPrefs

    val mockTwitter = Mockito.mock(Twitter::class.java)
    @Provides
    @Singleton
    fun provideTwitter(): Twitter = mockTwitter

    val mockOGClient = Mockito.mock(OpenGraphClient::class.java)
    @Provides
    @Singleton
    fun provideOpenGraphClient(): OpenGraphClient = mockOGClient
}
