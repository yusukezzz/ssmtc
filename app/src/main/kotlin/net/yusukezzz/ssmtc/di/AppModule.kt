package net.yusukezzz.ssmtc.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.TwitterApi
import net.yusukezzz.ssmtc.data.api.UploadApi
import net.yusukezzz.ssmtc.util.gson.DateTimeTypeConverter
import net.yusukezzz.ssmtc.util.okhttp.RetryWithDelayInterceptor
import okhttp3.OkHttpClient
import org.threeten.bp.OffsetDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer
import se.akerfeldt.okhttp.signpost.SigningInterceptor
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {
    companion object {
        const val API_BASE_URL = "https://api.twitter.com"
        const val UPLOAD_BASE_URL = "https://upload.twitter.com"
    }

    @Provides
    @Singleton
    fun provideApplication(): Application = app

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .registerTypeAdapter(OffsetDateTime::class.java, DateTimeTypeConverter())
        .create()

    @Provides
    @Singleton
    fun providePreferences(): Preferences = Preferences(app.applicationContext)

    @Provides
    @Singleton
    fun provideOauthConsumer(): OkHttpOAuthConsumer = OkHttpOAuthConsumer(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET)

    @Provides
    @Singleton
    fun provideOkhttp(oauthConsumer: OkHttpOAuthConsumer): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SigningInterceptor(oauthConsumer))
        .addInterceptor(RetryWithDelayInterceptor())
        .build()

    @Provides
    @Singleton
    fun provideRetrofitBuilder(gson: Gson, okhttp: OkHttpClient): Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okhttp)

    @Provides
    @Singleton
    fun provideApiService(builder: Retrofit.Builder): TwitterApi = builder.baseUrl(API_BASE_URL).build().create(TwitterApi::class.java)

    @Provides
    @Singleton
    fun provideUploadService(builder: Retrofit.Builder): UploadApi = builder.baseUrl(UPLOAD_BASE_URL).build().create(UploadApi::class.java)

    @Provides
    @Singleton
    fun provideTwitter(oauthConsumer: OkHttpOAuthConsumer, apiService: TwitterApi, uploadService: UploadApi): Twitter = Twitter(oauthConsumer, apiService, uploadService)
}
