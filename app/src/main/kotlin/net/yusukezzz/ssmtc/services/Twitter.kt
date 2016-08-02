package net.yusukezzz.ssmtc.services

import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.data.json.*
import net.yusukezzz.ssmtc.util.gson.GsonHolder
import net.yusukezzz.ssmtc.util.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer
import se.akerfeldt.okhttp.signpost.SigningInterceptor
import java.io.File

class Twitter {
    companion object {
        const val API_BASE_URL = "https://api.twitter.com"
        const val UPLOAD_BASE_URL = "https://upload.twitter.com"
        const val LANG = "ja"
        const val LOCALE = "ja"
        const val SEARCH_RESULT_TYPE = "recent"
    }

    private val oauthConsumer = OkHttpOAuthConsumer(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET)
    private val okhttp = OkHttpClient.Builder().addInterceptor(SigningInterceptor(oauthConsumer)).build()
    private val builder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(GsonHolder.gson))
        .client(okhttp)
    private val apiService = builder.baseUrl(API_BASE_URL).build().create(TwitterApi::class.java)
    private val uploadService = builder.baseUrl(UPLOAD_BASE_URL).build().create(UploadApi::class.java)

    fun setTokens(token: String?, tokenSecret: String?): Twitter {
        oauthConsumer.setTokenWithSecret(token, tokenSecret)

        return this
    }

    fun verifyCredentials(): User = execute(apiService.verifyCredentials())

    fun timeline(params: TimelineParameter): List<Tweet> {
        val maxId = params.maxId?.dec() // ignore same id

        return when (params.type) {
            TimelineParameter.TYPE_HOME -> homeTimeline(params.count, params.sinceId, maxId)
            TimelineParameter.TYPE_MENTIONS -> mentionsTimeline(params.count, params.sinceId, maxId)
            TimelineParameter.TYPE_LISTS -> listTimeline(params.listId, params.count, params.sinceId, params.maxId)
            TimelineParameter.TYPE_SEARCH -> searchTimeline(params.query, params.count, params.sinceId, maxId)
            TimelineParameter.TYPE_USER -> userTimeline(params.screenName, params.count, params.sinceId, maxId)
            else -> throw RuntimeException("unknown parameter type: ${params.javaClass}")
        }
    }

    fun like(id: Long): Tweet = execute(apiService.like(id))
    fun unlike(id: Long): Tweet = execute(apiService.unlike(id))
    fun retweet(id: Long): Tweet = execute(apiService.retweet(id))
    fun unretweet(id: Long): Tweet = execute(apiService.unretweet(id))

    fun subscribedLists(userId: Long): List<TwList> =
        execute(apiService.subscribedLists(1000, userId)).lists

    fun ownedLists(userId: Long): List<TwList> =
        execute(apiService.ownedLists(1000, userId)).lists

    fun tweet(status: String, inReplyToStatusId: Long? = null, mediaIds: List<Long>? = null): Tweet =
        execute(apiService.statusesUpdate(status, inReplyToStatusId, mediaIds))

    fun upload(media: File): UploadResult = execute(uploadService.upload(media.toRequestBody()))

    private fun homeTimeline(count: Int?, sinceId: Long?, maxId: Long?): List<Tweet> =
        execute(apiService.homeTimeline(count, sinceId, maxId))

    private fun mentionsTimeline(count: Int?, sinceId: Long?, maxId: Long?): List<Tweet> =
        execute(apiService.mentionsTimeline(count, sinceId, maxId))

    private fun directMessages(count: Int?, sinceId: Long?, maxId: Long?): List<Tweet> =
        execute(apiService.directMessages(count, sinceId, maxId))

    private fun listTimeline(listId: Long?, count: Int?, sinceId: Long?, maxId: Long?): List<Tweet> =
        execute(apiService.listStatuses(listId, count, sinceId, maxId))

    private fun searchTimeline(query: String?, count: Int?, sinceId: Long? = null, maxId: Long? = null): List<Tweet> =
        execute(apiService.search(count, LANG, LOCALE, SEARCH_RESULT_TYPE, query, sinceId, maxId)).statuses

    private fun userTimeline(screenName: String?, count: Int?, sinceId: Long? = null, maxId: Long? = null): List<Tweet> =
        execute(apiService.userTimeline(count, screenName, sinceId, maxId))

    private fun <T> execute(req: Call<T>): T {
        val res = req.execute()
        if (!res.isSuccessful) {
            handleError(res)
        }

        return res.body()
    }

    private fun <T> handleError(res: Response<T>) {
        val statusCode = res.code()
        val errRes = GsonHolder.gson.fromJson(res.errorBody().string(), TwitterErrorResponse::class.java)

        throw TwitterApiException("Twitter API error: status=$statusCode, cause=$errRes", statusCode, errRes.errors)
    }

    data class TwitterErrorResponse(val errors: List<TwitterErrorDetail>)
    data class TwitterErrorDetail(val code: Int, val message: String)
}

class TwitterApiException(message: String, val statusCode: Int, val errors: List<Twitter.TwitterErrorDetail>): Exception(message) {
    override fun toString(): String {
        val details = errors.map { "code=${it.code}, message=${it.message}" }.joinToString("\n")
        val str = """
        |[message]
        |$message
        |
        |[httpStatusCode]
        |$statusCode
        |
        |[errorDetails]
        |$details
        |
        |[stacktrace]
        |$stackTrace
        |
        """.trimMargin()

        return str
    }
}

interface TwitterApi {
    @GET("/1.1/account/verify_credentials.json")
    fun verifyCredentials(): Call<User>

    @GET("/1.1/statuses/home_timeline.json")
    fun homeTimeline(
        @Query("count") count: Int?,
        @Query("since_id") sinceId: Long?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/statuses/mentions_timeline.json")
    fun mentionsTimeline(
        @Query("count") count: Int?,
        @Query("since_id") sinceId: Long?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/direct_messages.json")
    fun directMessages(
        @Query("count") count: Int?,
        @Query("since_id") sinceId: Long?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/statuses/user_timeline.json")
    fun userTimeline(
        @Query("count") count: Int?,
        @Query("screen_name") screenName: String?,
        @Query("since_id") sinceId: Long?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/search/tweets.json")
    fun search(
        @Query("count") count: Int?,
        @Query("lang") lang: String,
        @Query("locale") locale: String,
        @Query("result_type") resultType: String,
        @Query("q") query: String?,
        @Query("since_id") sinceId: Long?,
        @Query("max_id") maxId: Long?
    ): Call<Search>

    @GET("/1.1/lists/subscriptions.json")
    fun subscribedLists(
        @Query("count") count: Int?,
        @Query("user_id") userId: Long
    ): Call<TwLists>

    @GET("/1.1/lists/ownerships.json")
    fun ownedLists(
        @Query("count") count: Int?,
        @Query("user_id") userId: Long
    ): Call<TwLists>

    @GET("/1.1/lists/statuses.json")
    fun listStatuses(
        @Query("list_id") listId: Long?,
        @Query("count") count: Int?,
        @Query("since_id") sinceId: Long?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @FormUrlEncoded
    @POST("/1.1/statuses/update.json")
    fun statusesUpdate(
        @Field("status") status: String,
        @Field("in_reply_to_status_id") inReplyToStatusId: Long?,
        @Field("media_ids") mediaIds: List<Long>?
    ): Call<Tweet>

    @POST("/1.1/statuses/retweet/{id}.json")
    fun retweet(@Path("id") id: Long): Call<Tweet>

    @POST("/1.1/statuses/unretweet/{id}.json")
    fun unretweet(@Path("id") id: Long): Call<Tweet>

    @POST("/1.1/favorites/create.json")
    fun like(@Query("id") id: Long): Call<Tweet>

    @POST("/1.1/favorites/destroy.json")
    fun unlike(@Query("id") id: Long): Call<Tweet>
}

interface UploadApi {
    @Multipart
    @POST("/1.1/media/upload.json")
    fun upload(
        @Part("media") file: RequestBody
    ): Call<UploadResult>
}
