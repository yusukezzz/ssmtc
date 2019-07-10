package net.yusukezzz.ssmtc.data.api

import com.google.gson.Gson
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.model.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer

class TwitterService(private val oauthConsumer: OkHttpOAuthConsumer,
                     private val apiService: TwitterApi,
                     private val uploadService: UploadApi,
                     private val gson: Gson) {
    companion object {
        const val API_BASE_URL = "https://api.twitter.com/1.1"
        const val UPLOAD_BASE_URL = "https://upload.twitter.com/1.1"
        const val LANG = "ja"
        const val LOCALE = "ja"
        const val SEARCH_RESULT_TYPE = "recent"
        const val MAX_RETRIEVE_COUNT = 200
    }

    fun setTokens(credentials: Credentials) {
        oauthConsumer.setTokenWithSecret(credentials.token, credentials.tokenSecret)
    }

    suspend fun verifyCredentials(): User = execute(apiService.verifyCredentials())

    suspend fun statuses(timeline: Timeline, maxId: Long? = null): List<Tweet> = timeline.let {
        val max = maxId?.dec()
        when (it.type) {
            Timeline.TYPE_HOME -> homeTimeline(max)
            Timeline.TYPE_MENTIONS -> mentionsTimeline(max)
            Timeline.TYPE_LISTS -> listTimeline(it.listId, max)
            Timeline.TYPE_SEARCH -> searchTimeline(SearchQueryBuilder.build(it), max)
            Timeline.TYPE_USER -> userTimeline(it.screenName, max)
            else -> throw RuntimeException("unknown timeline type: ${it.type::class.java}")
        }
    }

    suspend fun like(id: Long): Tweet = execute(apiService.like(id))
    suspend fun unlike(id: Long): Tweet = execute(apiService.unlike(id))
    suspend fun retweet(id: Long): Tweet = execute(apiService.retweet(id))
    suspend fun unretweet(id: Long): Tweet = execute(apiService.unretweet(id))

    suspend fun subscribedLists(userId: Long): List<TwList> =
        execute(apiService.subscribedLists(1000, userId)).lists

    suspend fun ownedLists(userId: Long): List<TwList> =
        execute(apiService.ownedLists(1000, userId)).lists

    suspend fun blockedIds(): IdList = execute(apiService.blockedIds())
    suspend fun mutedIds(): IdList = execute(apiService.mutedIds())

    fun tweet(status: String, inReplyToStatusId: Long? = null, mediaIds: List<Long>? = null): Tweet =
        execute(apiService.statusesUpdate(status, inReplyToStatusId, mediaIds?.joinToString(",")))

    fun upload(media: RequestBody): UploadResult = execute(uploadService.upload(media))

    fun uploadInit(totalBytes: Long): UploadResult = execute(uploadService.init(totalBytes))
    fun uploadAppend(mediaId: Long, segmentIndex: Int, chunk: RequestBody) =
        execute(uploadService.append(mediaId, segmentIndex, chunk))

    fun uploadFinalize(mediaId: Long): UploadResult = execute(uploadService.finalize(mediaId))
    fun uploadStatus(mediaId: Long): UploadResult = execute(uploadService.status(mediaId))

    private suspend fun homeTimeline(maxId: Long?): List<Tweet> =
        execute(apiService.homeTimeline(MAX_RETRIEVE_COUNT, maxId))

    private suspend fun mentionsTimeline(maxId: Long?): List<Tweet> =
        execute(apiService.mentionsTimeline(MAX_RETRIEVE_COUNT, maxId))

    private suspend fun listTimeline(listId: Long?, maxId: Long?): List<Tweet> =
        execute(apiService.listStatuses(listId, MAX_RETRIEVE_COUNT, maxId))

    private suspend fun searchTimeline(query: String?, maxId: Long?): List<Tweet> =
        execute(apiService.search(MAX_RETRIEVE_COUNT, LANG, LOCALE, SEARCH_RESULT_TYPE, query, maxId)).statuses

    private suspend fun userTimeline(screenName: String?, maxId: Long?): List<Tweet> =
        execute(apiService.userTimeline(MAX_RETRIEVE_COUNT, screenName, maxId))

    private fun <T> execute(res: Response<T>): T {
        if (!res.isSuccessful) {
            handleError(res)
        }

        return res.body()!!
    }

    private fun handleError(res: Response<*>) {
        val statusCode = res.code()
        val body = gson.fromJson(res.errorBody()!!.string(), TwitterErrorResponse::class.java)
        throw TwitterApiException("twitter API error code=$statusCode", statusCode, body.errors)
    }

    data class TwitterErrorResponse(val errors: List<TwitterErrorDetail>)
    data class TwitterErrorDetail(val code: Int, val message: String)
}

class TwitterApiException(message: String, val statusCode: Int, val errors: List<TwitterService.TwitterErrorDetail>) : RuntimeException(message) {
    companion object {
        const val STATUS_CODE_RATE_LIMIT = 429
    }

    fun isRateLimitExceeded(): Boolean = (statusCode == STATUS_CODE_RATE_LIMIT)
    override fun toString(): String {
        val details = errors.joinToString("\n") { "code=${it.code}, message=${it.message}" }

        return """
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
    }
}

interface TwitterApi {
    @GET("/account/verify_credentials.json")
    suspend fun verifyCredentials(): Response<User>

    @GET("/statuses/home_timeline.json?tweet_mode=extended")
    suspend fun homeTimeline(
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("/statuses/mentions_timeline.json?tweet_mode=extended")
    suspend fun mentionsTimeline(
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("/statuses/user_timeline.json?tweet_mode=extended")
    suspend fun userTimeline(
        @Query("count") count: Int?,
        @Query("screen_name") screenName: String?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("/search/tweets.json?tweet_mode=extended")
    suspend fun search(
        @Query("count") count: Int?,
        @Query("lang") lang: String,
        @Query("locale") locale: String,
        @Query("result_type") resultType: String,
        @Query("q", encoded = true) query: String?,
        @Query("max_id") maxId: Long?
    ): Response<Search>

    @GET("/lists/subscriptions.json")
    suspend fun subscribedLists(
        @Query("count") count: Int?,
        @Query("user_id") userId: Long
    ): Response<TwLists>

    @GET("/lists/ownerships.json")
    suspend fun ownedLists(
        @Query("count") count: Int?,
        @Query("user_id") userId: Long
    ): Response<TwLists>

    @GET("/lists/statuses.json?tweet_mode=extended")
    suspend fun listStatuses(
        @Query("list_id") listId: Long?,
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("/blocks/ids.json")
    suspend fun blockedIds(): Response<IdList>

    @GET("/mutes/users/ids.json")
    suspend fun mutedIds(): Response<IdList>

    @FormUrlEncoded
    @POST("/statuses/update.json")
    fun statusesUpdate(
        @Field("status") status: String,
        @Field("in_reply_to_status_id") inReplyToStatusId: Long?,
        @Field("media_ids") mediaIds: String?
    ): Response<Tweet>

    @POST("/statuses/retweet/{id}.json")
    suspend fun retweet(@Path("id") id: Long): Response<Tweet>

    @POST("/statuses/unretweet/{id}.json")
    suspend fun unretweet(@Path("id") id: Long): Response<Tweet>

    @POST("/favorites/create.json")
    suspend fun like(@Query("id") id: Long): Response<Tweet>

    @POST("/favorites/destroy.json")
    suspend fun unlike(@Query("id") id: Long): Response<Tweet>
}

interface UploadApi {
    @Multipart
    @POST("/media/upload.json")
    fun upload(
        @Part("media") file: RequestBody
    ): Response<UploadResult>

    @Multipart
    @POST("/media/upload.json?command=INIT&media_type=video/mp4&media_category=tweet_video")
    fun init(
        @Query("total_bytes") totalBytes: Long
    ): Response<UploadResult>

    @Multipart
    @POST("/media/upload.json?command=APPEND")
    fun append(
        @Query("media_id") mediaId: Long,
        @Query("segment_index") segmentIndex: Int,
        @Part("media") file: RequestBody
    ): Response<Unit>

    @Multipart
    @POST("/media/upload.json?command=FINALIZE")
    fun finalize(
        @Query("media_id") mediaId: Long
    ): Response<UploadResult>

    @GET("/media/upload.json?command=STATUS")
    fun status(
        @Query("media_id") mediaId: Long
    ): Response<UploadResult>
}
