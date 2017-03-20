package net.yusukezzz.ssmtc.di

import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import id.zelory.compressor.Compressor
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.Twitter.Companion.API_BASE_URL
import net.yusukezzz.ssmtc.data.api.Twitter.Companion.UPLOAD_BASE_URL
import net.yusukezzz.ssmtc.data.api.TwitterApi
import net.yusukezzz.ssmtc.data.api.UploadApi
import net.yusukezzz.ssmtc.data.og.OGDiskCache
import net.yusukezzz.ssmtc.data.og.OpenGraphClient
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
    companion object {
        const val PHOTO_MAX_WIDTH: Float = 2048.0f
        const val PHOTO_MAX_HEIGHT: Float = 1536.0f
        const val PHOTO_QUALITY: Int = 85
    }

    @Provides
    @Singleton
    fun provideApplication(): Application = app

    @Provides
    @Named("cacheDir")
    fun provideCacheDir(app: Application): File = app.cacheDir

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .registerTypeAdapter(OffsetDateTime::class.java, DateTimeTypeConverter())
        .create()

    @Provides
    @Singleton
    fun providePreferences(gson: Gson): Preferences = Preferences(app.applicationContext, gson)

    @Provides
    @Singleton
    fun provideOauthConsumer(): OkHttpOAuthConsumer = OkHttpOAuthConsumer(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET)

    @Provides
    @Singleton
    fun provideTwitterOkhttp(oauthConsumer: OkHttpOAuthConsumer): OkHttpClient = OkHttpClient.Builder()
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

    @Provides
    @Singleton
    fun provideOGDiskCache(@Named("cacheDir") cacheDir: File, gson: Gson): OGDiskCache = OGDiskCache(cacheDir, gson)

    @Provides
    @Singleton
    fun provideOpenGraphClient(cache: OGDiskCache): OpenGraphClient = OpenGraphClient(cache, OkHttpClient.Builder().build())

    @Provides
    @Singleton
    fun provideCompressor(): Compressor = Compressor.Builder(app.applicationContext)
        .setMaxWidth(PHOTO_MAX_WIDTH)
        .setMaxHeight(PHOTO_MAX_HEIGHT)
        .setQuality(PHOTO_QUALITY)
        .setCompressFormat(Bitmap.CompressFormat.JPEG) // should respect original?
        .build()
}
