package net.yusukezzz.ssmtc.data.api

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import net.yusukezzz.ssmtc.data.api.FilterRule.Showing.ALL
import net.yusukezzz.ssmtc.data.api.model.Tweet
import java.util.regex.Pattern

@Parcelize
data class FilterRule(
    val showing: FilterRule.Showing,
    val includeWords: List<String>,
    val excludeWords: List<String>
) : Parcelable {
    companion object {
        fun default(): FilterRule = FilterRule(ALL, listOf(), listOf())
    }

    enum class Showing {
        ALL, ANY_MEDIA, PHOTO, VIDEO
    }

    fun match(tweet: Tweet): Boolean = matchMedia(tweet) && matchText(tweet)

    private fun matchMedia(tweet: Tweet): Boolean = when (showing) {
        Showing.ALL -> true // include those without media
        Showing.ANY_MEDIA -> tweet.hasPhoto || tweet.hasVideo
        Showing.PHOTO -> tweet.hasPhoto
        Showing.VIDEO -> tweet.hasVideo
    }

    private fun matchText(tweet: Tweet): Boolean {
        if (excludeWords.isNotEmpty() && excludeWords.toRegex().containsMatchIn(tweet.full_text)) return false

        if (includeWords.isEmpty()) return true

        if (includeWords.toRegex().containsMatchIn(tweet.full_text)) return true

        return false
    }

    private fun List<String>.toRegex(): Regex = Regex(this.map(Pattern::quote).joinToString("|"), RegexOption.IGNORE_CASE)
}
