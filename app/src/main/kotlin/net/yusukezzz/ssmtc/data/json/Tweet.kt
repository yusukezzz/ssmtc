package net.yusukezzz.ssmtc.data.json

import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable
import org.joda.time.DateTime

@PaperParcel
data class Tweet(
    val id: Long,
    val text: String,
    val user: User,
    val entities: Entity,
    val extended_entities: Entity?,
    val created_at: DateTime,
    val retweeted_status: Tweet?,
    val quoted_status: Tweet?,
    var retweet_count: Int,
    var favorite_count: Int,
    var retweeted: Boolean,
    var favorited: Boolean
) : PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Tweet::class.java)
    }

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
