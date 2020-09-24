package net.yusukezzz.ssmtc.data.api

import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.api.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer

class TwitterService(
    private val oauthConsumer: OkHttpOAuthConsumer,
    private val apiService: TwitterApi,
    private val uploadService: UploadApi
) {
    companion object {
        const val API_BASE_URL = "https://api.twitter.com/1.1/"
        const val UPLOAD_BASE_URL = "https://upload.twitter.com/1.1/"
        const val LANG = "ja"
        const val LOCALE = "ja"
        const val SEARCH_RESULT_TYPE = "recent"
        const val MAX_RETRIEVE_COUNT = 200
    }

    fun setTokens(credentials: Credentials) {
        oauthConsumer.setTokenWithSecret(credentials.token, credentials.tokenSecret)
    }

    suspend fun verifyCredentials(): TwitterApiResult<User> = handleResponse(apiService.verifyCredentials())

    suspend fun statuses(timeline: Timeline, maxId: Long? = null): TwitterApiResult<List<Tweet>> = timeline.let {
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

    suspend fun like(id: Long): TwitterApiResult<Tweet> = handleResponse(apiService.like(id))
    suspend fun unlike(id: Long): TwitterApiResult<Tweet> = handleResponse(apiService.unlike(id))
    suspend fun retweet(id: Long): TwitterApiResult<Tweet> = handleResponse(apiService.retweet(id))
    suspend fun unretweet(id: Long): TwitterApiResult<Tweet> = handleResponse(apiService.unretweet(id))

    suspend fun subscribedLists(userId: Long): TwitterApiResult<TwLists> =
        handleResponse(apiService.subscribedLists(1000, userId))

    suspend fun ownedLists(userId: Long): TwitterApiResult<TwLists> =
        handleResponse(apiService.ownedLists(1000, userId))

    suspend fun blockedIds(): TwitterApiResult<IdList> = handleResponse(apiService.blockedIds())
    suspend fun mutedIds(): TwitterApiResult<IdList> = handleResponse(apiService.mutedIds())

    fun tweet(
        status: String,
        inReplyToStatusId: Long? = null,
        mediaIds: List<Long>? = null
    ): TwitterApiResult<Tweet> =
        handleCall(
            apiService.statusesUpdate(
                status,
                inReplyToStatusId,
                mediaIds?.joinToString(",")
            )
        )

    fun upload(media: RequestBody): TwitterApiResult<UploadResult> = handleCall(uploadService.upload(media))

    private val textType = "text/plain".toMediaType()
    fun uploadInit(totalBytes: Long): TwitterApiResult<UploadResult> =
        handleCall(uploadService.init(totalBytes))

    fun uploadAppend(mediaId: Long, segmentIndex: Int, chunk: RequestBody) =
            uploadService.append(
                mediaId.toString().toRequestBody(textType),
                segmentIndex.toString().toRequestBody(textType),
                chunk
            ).execute()

    fun uploadFinalize(mediaId: Long): TwitterApiResult<UploadResult> =
        handleCall(uploadService.finalize(mediaId))

    fun uploadStatus(mediaId: Long): TwitterApiResult<UploadResult> =
        handleCall(uploadService.status(mediaId))

    private suspend fun homeTimeline(maxId: Long?): TwitterApiResult<List<Tweet>> =
        handleResponse(apiService.homeTimeline(MAX_RETRIEVE_COUNT, maxId))

    private suspend fun mentionsTimeline(maxId: Long?): TwitterApiResult<List<Tweet>> =
        handleResponse(apiService.mentionsTimeline(MAX_RETRIEVE_COUNT, maxId))

    private suspend fun listTimeline(listId: Long?, maxId: Long?): TwitterApiResult<List<Tweet>> =
        handleResponse(apiService.listStatuses(listId, MAX_RETRIEVE_COUNT, maxId))

    private suspend fun searchTimeline(query: String?, maxId: Long?): TwitterApiResult<List<Tweet>> {
        val res = handleResponse(
            apiService.search(
                MAX_RETRIEVE_COUNT,
                LANG,
                LOCALE,
                SEARCH_RESULT_TYPE,
                query,
                maxId
            )
        )
        // unwrap Search result
        return TwitterApiResult(res.data?.statuses, res.error)
    }

    private suspend fun userTimeline(screenName: String?, maxId: Long?): TwitterApiResult<List<Tweet>> =
        handleResponse(apiService.userTimeline(MAX_RETRIEVE_COUNT, screenName, maxId))

    private fun <T> handleCall(req: Call<T>): TwitterApiResult<T> = handleResponse(req.execute())

    private fun <T> handleResponse(res: Response<T>): TwitterApiResult<T> =
        if (res.isSuccessful) {
            TwitterApiResult(res.body())
        } else {
            val url = res.raw().request.url.toString()
            val statusCode = res.code()
            val json = res.errorBody()?.string() ?: ""
            val error = TwitterApiError(url, statusCode, json)
            TwitterApiResult(error = error)
        }
}

class TwitterApiError(
    val url: String,
    val statusCode: Int,
    val json: String
) : RuntimeException() {
    companion object {
        const val STATUS_CODE_RATE_LIMIT = 429
    }

    fun isRateLimitExceeded(): Boolean = (statusCode == STATUS_CODE_RATE_LIMIT)
    override fun toString(): String {
        return """
        |[url]
        |$url
        |
        |[status code]
        |$statusCode
        |
        |[error json]
        |$json
        """.trimMargin()
    }
}

data class TwitterApiResult<T>(
    val data: T? = null,
    val error: TwitterApiError? = null
)

interface TwitterApi {
    @GET("account/verify_credentials.json")
    suspend fun verifyCredentials(): Response<User>

    @GET("statuses/home_timeline.json?tweet_mode=extended")
    suspend fun homeTimeline(
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("statuses/mentions_timeline.json?tweet_mode=extended")
    suspend fun mentionsTimeline(
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("statuses/user_timeline.json?tweet_mode=extended")
    suspend fun userTimeline(
        @Query("count") count: Int?,
        @Query("screen_name") screenName: String?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("search/tweets.json?tweet_mode=extended")
    suspend fun search(
        @Query("count") count: Int?,
        @Query("lang") lang: String,
        @Query("locale") locale: String,
        @Query("result_type") resultType: String,
        @Query("q", encoded = true) query: String?,
        @Query("max_id") maxId: Long?
    ): Response<Search>

    @GET("lists/subscriptions.json")
    suspend fun subscribedLists(
        @Query("count") count: Int?,
        @Query("user_id") userId: Long
    ): Response<TwLists>

    @GET("lists/ownerships.json")
    suspend fun ownedLists(
        @Query("count") count: Int?,
        @Query("user_id") userId: Long
    ): Response<TwLists>

    @GET("lists/statuses.json?tweet_mode=extended")
    suspend fun listStatuses(
        @Query("list_id") listId: Long?,
        @Query("count") count: Int?,
        @Query("max_id") maxId: Long?
    ): Response<List<Tweet>>

    @GET("blocks/ids.json")
    suspend fun blockedIds(): Response<IdList>

    @GET("mutes/users/ids.json")
    suspend fun mutedIds(): Response<IdList>

    @FormUrlEncoded
    @POST("statuses/update.json")
    fun statusesUpdate(
        @Field("status") status: String,
        @Field("in_reply_to_status_id") inReplyToStatusId: Long?,
        @Field("media_ids") mediaIds: String?
    ): Call<Tweet>

    @POST("statuses/retweet/{id}.json")
    suspend fun retweet(@Path("id") id: Long): Response<Tweet>

    @POST("statuses/unretweet/{id}.json")
    suspend fun unretweet(@Path("id") id: Long): Response<Tweet>

    @POST("favorites/create.json")
    suspend fun like(@Query("id") id: Long): Response<Tweet>

    @POST("favorites/destroy.json")
    suspend fun unlike(@Query("id") id: Long): Response<Tweet>
}

interface UploadApi {
    @Multipart
    @POST("media/upload.json")
    fun upload(
        @Part("media") file: RequestBody
    ): Call<UploadResult>

    @FormUrlEncoded
    @POST("media/upload.json?command=INIT&media_type=video%2Fmp4&media_category=tweet_video")
    fun init(
        @Field("total_bytes") totalBytes: Long
    ): Call<UploadResult>

    @Multipart
    @POST("media/upload.json")
    fun append(
        @Part("media_id") mediaId: RequestBody,
        @Part("segment_index") segmentIndex: RequestBody,
        @Part("media") media: RequestBody,
        @Part("command") command: RequestBody = "APPEND".toRequestBody("text/plain".toMediaType())
    ): Call<Unit>

    @FormUrlEncoded
    @POST("media/upload.json?command=FINALIZE")
    fun finalize(
        @Field("media_id") mediaId: Long
    ): Call<UploadResult>

    @GET("media/upload.json?command=STATUS")
    fun status(
        @Query("media_id") mediaId: Long
    ): Call<UploadResult>
}
