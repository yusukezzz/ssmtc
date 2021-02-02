package net.yusukezzz.ssmtc.data.api.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.OffsetDateTime

@Parcelize
data class Tweet(
    val id: Long,
    val full_text: String,
    val user: User,
    val entities: Entity,
    val extended_entities: Entity?,
    val created_at: OffsetDateTime,
    val retweeted_status: Tweet?,
    val quoted_status: Tweet?,
    var retweet_count: Int,
    var favorite_count: Int,
    var retweeted: Boolean,
    var favorited: Boolean
) : Parcelable {
    val permalinkUrl: String
        get() = "https://twitter.com/${user.screenName}/status/$id"

    val allMedia: List<Media>
        get() = extended_entities?.media ?: listOf()

    val photos: List<Media>
        get() = allMedia.filter { it.isPhoto }

    val videos: List<Media>
        get() = allMedia.filter { it.isVideo }

    val hasPhoto: Boolean
        get() = photos.isNotEmpty()

    val hasVideo: Boolean
        get() = videos.isNotEmpty()

    val isRetweet: Boolean
        get() = (null != retweeted_status)

    val isRetweetWithQuoted: Boolean
        get() = (null != quoted_status)
}
