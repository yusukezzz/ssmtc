package net.yusukezzz.ssmtc.di

import android.accounts.AccountManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.SlackApi
import net.yusukezzz.ssmtc.data.SlackService
import net.yusukezzz.ssmtc.data.SlackService.Companion.SLACK_BASE_URL
import net.yusukezzz.ssmtc.data.api.TwitterApi
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.api.TwitterService.Companion.API_BASE_URL
import net.yusukezzz.ssmtc.data.api.TwitterService.Companion.UPLOAD_BASE_URL
import net.yusukezzz.ssmtc.data.api.UploadApi
import net.yusukezzz.ssmtc.data.og.OGDiskCache
import net.yusukezzz.ssmtc.data.og.OpenGraphApi
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.data.repository.TimelineRepository
import net.yusukezzz.ssmtc.util.gson.DateTimeTypeConverter
import net.yusukezzz.ssmtc.util.okhttp.RetryWithDelayInterceptor
import okhttp3.OkHttpClient
import org.threeten.bp.OffsetDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer
import se.akerfeldt.okhttp.signpost.SigningInterceptor
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {
    @Provides
    @Singleton
    fun provideApplication(): Application = app

    @Provides
    @Singleton
    @Named("cacheDir")
    fun provideCacheDir(app: Application): File = app.cacheDir

    @Provides
    @Singleton
    @Named("filesDir")
    fun provideFilesDir(app: Application): File = app.filesDir

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
    fun provideOauthConsumer(): OkHttpOAuthConsumer =
        OkHttpOAuthConsumer(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET)

    @Provides
    @Singleton
    @Named("okHttp")
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(RetryWithDelayInterceptor())
        .build()

    @Provides
    @Singleton
    @Named("twitterOkHttp")
    fun provideTwitterOkhttp(oauthConsumer: OkHttpOAuthConsumer): OkHttpClient =
        OkHttpClient.Builder()
            // .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
            .addInterceptor(SigningInterceptor(oauthConsumer))
            .addInterceptor(RetryWithDelayInterceptor())
            .build()

    @Provides
    @Singleton
    @Named("retrofitBuilder")
    fun provideRetrofitBuilder(
        gson: Gson,
        @Named("okHttp") okhttp: OkHttpClient
    ): Retrofit.Builder =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okhttp)

    @Provides
    @Singleton
    @Named("twitterRetrofitBuilder")
    fun provideTwitterRetrofitBuilder(
        gson: Gson,
        @Named("twitterOkHttp") okhttp: OkHttpClient
    ): Retrofit.Builder =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okhttp)

    @Provides
    @Singleton
    fun provideTwitterApi(
        @Named("twitterRetrofitBuilder") builder: Retrofit.Builder
    ): TwitterApi =
        builder.baseUrl(API_BASE_URL).build().create(TwitterApi::class.java)

    @Provides
    @Singleton
    fun provideTwitterUploadApi(
        @Named("twitterRetrofitBuilder") builder: Retrofit.Builder
    ): UploadApi =
        builder.baseUrl(UPLOAD_BASE_URL).build().create(UploadApi::class.java)

    @Provides
    @Singleton
    fun provideTwitterService(
        oauthConsumer: OkHttpOAuthConsumer,
        apiService: TwitterApi,
        uploadService: UploadApi
    ): TwitterService = TwitterService(oauthConsumer, apiService, uploadService)

    @Provides
    @Singleton
    fun provideOGDiskCache(@Named("cacheDir") cacheDir: File, gson: Gson): OGDiskCache =
        OGDiskCache(cacheDir, gson)

    @Provides
    @Singleton
    fun provideOpenGraphApi(@Named("retrofitBuilder") builder: Retrofit.Builder): OpenGraphApi =
        builder.baseUrl(BuildConfig.MY_API_BASE_URL).build().create(OpenGraphApi::class.java)

    @Provides
    @Singleton
    fun provideSlackService(@Named("retrofitBuilder") builder: Retrofit.Builder): SlackService =
        SlackService(
            BuildConfig.SLACK_TOKEN,
            builder.baseUrl(SLACK_BASE_URL).build().create(SlackApi::class.java)
        )

    @Provides
    @Singleton
    fun provideTimelineRepository(
        @Named("filesDir") filesDir: File,
        gson: Gson
    ): TimelineRepository =
        TimelineRepository(filesDir, gson)

    @Provides
    @Singleton
    fun provideAccountManager(): AccountManager = AccountManager.get(app.applicationContext)

    @Provides
    @Singleton
    fun provideSsmtcAccountRepository(
        am: AccountManager,
        gson: Gson,
        timelineRepository: TimelineRepository
    ): SsmtcAccountRepository = SsmtcAccountRepository(am, gson, timelineRepository)
}
