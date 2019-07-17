package net.yusukezzz.ssmtc.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * TwitterService user id list for block and mute ids API
 */
data class IdList(
    val ids: List<Long>,
    @SerializedName("next_cursor") val nextCursor: Long,
    @SerializedName("previous_cursor") val previousCursor: Long
)
