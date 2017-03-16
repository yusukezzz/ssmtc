package net.yusukezzz.ssmtc.data.api

import net.yusukezzz.ssmtc.data.api.model.*
import net.yusukezzz.ssmtc.util.toRequestBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer
import java.io.File

open class Twitter(private val oauthConsumer: OkHttpOAuthConsumer,
                   private val apiService: TwitterApi,
                   private val uploadService: UploadApi) {
    companion object {
        const val API_BASE_URL = "https://api.twitter.com"
        const val UPLOAD_BASE_URL = "https://upload.twitter.com"
        const val LANG = "ja"
        const val LOCALE = "ja"
        const val SEARCH_RESULT_TYPE = "recent"
    }

    fun setTokens(token: String?, tokenSecret: String?): Twitter {
        oauthConsumer.setTokenWithSecret(token, tokenSecret)

        return this
    }

    fun verifyCredentials(): User = execute(apiService.verifyCredentials())

    fun timeline(params: TimelineParameter, maxId: Long? = null): List<Tweet> = params.let {
        val max = maxId?.dec()
        when (it.type) {
            TimelineParameter.TYPE_HOME -> homeTimeline(it, max)
            TimelineParameter.TYPE_MENTIONS -> mentionsTimeline(it, max)
            TimelineParameter.TYPE_LISTS -> listTimeline(it, max)
            TimelineParameter.TYPE_SEARCH -> searchTimeline(SearchQueryBuilder.build(it), max)
            TimelineParameter.TYPE_USER -> userTimeline(it, max)
            else -> throw RuntimeException("unknown parameter type: ${it.type::class.java}")
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

    fun blockedIds(): IdList = execute(apiService.blockedIds())
    fun mutedIds(): IdList = execute(apiService.mutedIds())

    fun tweet(status: String, inReplyToStatusId: Long? = null, mediaIds: List<Long>? = null): Tweet =
        execute(apiService.statusesUpdate(status, inReplyToStatusId, mediaIds?.joinToString(",")))

    fun upload(media: File): UploadResult = execute(uploadService.upload(media.toRequestBody()))

    private fun homeTimeline(params: TimelineParameter, maxId: Long?): List<Tweet> =
        execute(apiService.homeTimeline(params.count, maxId))

    private fun mentionsTimeline(params: TimelineParameter, maxId: Long?): List<Tweet> =
        execute(apiService.mentionsTimeline(params.count, maxId))

    private fun listTimeline(params: TimelineParameter, maxId: Long?): List<Tweet> =
        execute(apiService.listStatuses(params.listId, params.count, maxId))

    private fun searchTimeline(params: TimelineParameter, maxId: Long?): List<Tweet> =
        execute(apiService.search(params.count, LANG, LOCALE, SEARCH_RESULT_TYPE, params.query, maxId)).statuses

    private fun userTimeline(params: TimelineParameter, maxId: Long?): List<Tweet> =
        execute(apiService.userTimeline(params.count, params.screenName, maxId))

    private fun <T> execute(req: Call<T>): T {
        val res = req.execute()
        if (!res.isSuccessful) {
            handleError(res)
        }

        return res.body()
    }

    private fun handleError(res: Response<*>) {
        val statusCode = res.code()
        RuntimeException("twitter API error code=$statusCode")
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
        |${stackTrace.joinToString("\n")}
        |
        """.trimMargin()

        return str
    }
}

interface TwitterApi {
    @GET("/1.1/account/verify_credentials.json")
    fun verifyCredentials(): Call<User>

    @GET("/1.1/statuses/home_timeline.json?tweet_mode=extended")
    fun homeTimeline(
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/statuses/mentions_timeline.json?tweet_mode=extended")
    fun mentionsTimeline(
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/statuses/user_timeline.json?tweet_mode=extended")
    fun userTimeline(
        @Query("count") count: Int?,
        @Query("screen_name") screenName: String?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/search/tweets.json?tweet_mode=extended")
    fun search(
        @Query("count") count: Int?,
        @Query("lang") lang: String,
        @Query("locale") locale: String,
        @Query("result_type") resultType: String,
        @Query("q", encoded = true) query: String?,
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

    @GET("/1.1/lists/statuses.json?tweet_mode=extended")
    fun listStatuses(
        @Query("list_id") listId: Long?,
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Call<List<Tweet>>

    @GET("/1.1/blocks/ids.json")
    fun blockedIds(): Call<IdList>

    @GET("/1.1/mutes/users/ids.json")
    fun mutedIds(): Call<IdList>

    @FormUrlEncoded
    @POST("/1.1/statuses/update.json")
    fun statusesUpdate(
        @Field("status") status: String,
        @Field("in_reply_to_status_id") inReplyToStatusId: Long?,
        @Field("media_ids") mediaIds: String?
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
