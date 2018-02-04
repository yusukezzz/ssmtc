package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.util.truncateBytes
import org.threeten.bp.OffsetDateTime
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

class SlackService(private val token: String, private val api: SlackApi) {
    companion object {
        const val SLACK_BASE_URL = "https://slack.com"
        private const val MAX_MESSAGE_LENGTH = 4000
    }

    fun sendMessage(e: Throwable, channel: String): SlackUploadResult {
        val cause = e.cause?.let { "\n[cause] ${it.message}\n${it.stackTrace.joinToString("\n")}" } ?: ""
        val mes = """|```[error] ${OffsetDateTime.now()}
            |${e.message}
            |${e.stackTrace.joinToString("\n")}
            |$cause```
        """.trimMargin()
        return sendMessage(mes, channel)
    }

    fun sendMessage(mes: String, channel: String): SlackUploadResult = api.postMessage(token, channel, mes.truncateBytes(MAX_MESSAGE_LENGTH)).execute().body()!!
}

data class SlackUploadResult(val ok: Boolean, val error: String)

interface SlackApi {
    @GET("/api/chat.postMessage")
    fun postMessage(
        @Query("token") token: String,
        @Query("channel") channel: String,
        @Query("text") content: String): Call<SlackUploadResult>
}
