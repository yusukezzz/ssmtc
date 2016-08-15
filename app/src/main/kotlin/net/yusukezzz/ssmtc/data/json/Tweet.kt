package net.yusukezzz.ssmtc.data.json

import nz.bradcampbell.paperparcel.PaperParcel
import org.joda.time.DateTime

@PaperParcel
data class Tweet(
    val id: Long,
    val text: String,
    val user: User,
    val entities: Entity,
    val extended_entities: Entity?,
    val created_at: DateTime,
    var retweet_count: Int,
    var favorite_count: Int,
    var retweeted: Boolean,
    var favorited: Boolean,
    val retweeted_status: Tweet?,
    val quoted_status: Tweet?
) {
    val allMedia: List<Media>
        get() = extended_entities?.media ?: listOf()

    val hasPhoto: Boolean
        get() {
            return if (allMedia.size == 0) {
                false
            } else {
                allMedia[0].isPhoto
            }
        }

    val hasVideo: Boolean
        get() {
            return if (allMedia.size == 0) {
                false
            } else {
                allMedia[0].isVideo
            }
        }

    val isRetweet: Boolean
        get() = (null != retweeted_status)

    val isRetweetWithQuoted: Boolean
        get() = (null != quoted_status)
}
