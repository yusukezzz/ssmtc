package net.yusukezzz.ssmtc.services

import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.services.TimelineFilter.Showing.*
import nz.bradcampbell.paperparcel.PaperParcel
import java.util.regex.Pattern

@PaperParcel
data class TimelineFilter(
    val showing: Showing,
    val includeWords: List<String>,
    val excludeWords: List<String>
) {
    companion object {
        fun default(): TimelineFilter = TimelineFilter(ALL, listOf(), listOf())
    }

    enum class Showing {
        ALL, ANY_MEDIA, PHOTO, VIDEO
    }

    fun apply(tweets: List<Tweet>): List<Tweet> = tweets.map {
        it.apply { it.visible = filterMedia(it) && filterText(it) }
    }

    private fun filterMedia(tweet: Tweet): Boolean = when (showing) {
        ALL -> true // include without media
        ANY_MEDIA -> tweet.hasPhoto || tweet.hasVideo
        PHOTO -> tweet.hasPhoto
        VIDEO -> tweet.hasVideo
    }

    private fun filterText(tweet: Tweet): Boolean {
        if (excludeWords.isNotEmpty() && excludeWords.toRegex().containsMatchIn(tweet.text)) return false

        if (includeWords.isEmpty()) return true

        if (includeWords.toRegex().containsMatchIn(tweet.text)) return true

        return false
    }

    private fun List<String>.toRegex(): Regex = Regex(this.map { Pattern.quote(it) }.joinToString("|"))
}
