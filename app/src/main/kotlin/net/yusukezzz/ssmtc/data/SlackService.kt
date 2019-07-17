package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.util.prettyMarkdown
import net.yusukezzz.ssmtc.util.truncateBytes
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

class SlackService(private val token: String, private val api: SlackApi) {
    companion object {
        const val SLACK_BASE_URL = "https://slack.com"
        private const val MAX_MESSAGE_LENGTH = 4000
    }

    fun sendMessage(mes: String, channel: String): SlackUploadResult =
        api.postMessage(token, channel, mes.truncateBytes(MAX_MESSAGE_LENGTH)).execute().body()!!

    fun sendMessage(e: Throwable, channel: String): SlackUploadResult = sendMessage(e.prettyMarkdown(), channel)
}

data class SlackUploadResult(val ok: Boolean, val error: String)

interface SlackApi {
    @GET("/api/chat.postMessage")
    fun postMessage(
        @Query("token") token: String,
        @Query("channel") channel: String,
        @Query("text") content: String
    ): Call<SlackUploadResult>
}
