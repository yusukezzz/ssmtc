package net.yusukezzz.ssmtc.data.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.yusukezzz.ssmtc.data.api.model.Tweet
import java.util.regex.Pattern

@Parcelize
data class FilterRule(
    val showing: ContentShowing,
    val includeWords: List<String>,
    val excludeWords: List<String>
) : Parcelable {
    companion object {
        fun default(): FilterRule = FilterRule(ContentShowing.ALL, listOf(), listOf())
    }

    fun match(tweet: Tweet): Boolean {
        val tweetMatched = matchMedia(tweet) && matchText(tweet)
        val retweetMatched = tweet.retweeted_status?.let { matchMedia(it) && matchText(it) } == true
        val quotedMatched = tweet.quoted_status?.let { matchMedia(it) && matchText(it) } == true

        return tweetMatched || retweetMatched || quotedMatched
    }

    private fun matchMedia(tweet: Tweet): Boolean = when (showing) {
        ContentShowing.ALL -> true
        ContentShowing.ANY_MEDIA -> tweet.hasPhoto || tweet.hasVideo
        ContentShowing.PHOTO -> tweet.hasPhoto
        ContentShowing.VIDEO -> tweet.hasVideo
    }

    private fun matchText(tweet: Tweet): Boolean {
        val text = tweet.user.name + tweet.user.screenName + tweet.full_text
        if (excludeWords.isNotEmpty() && excludeWords.toRegex().containsMatchIn(text)) return false

        if (includeWords.isEmpty()) return true

        if (includeWords.toRegex().containsMatchIn(text)) return true

        return false
    }

    private fun List<String>.toRegex(): Regex =
        Regex(this.joinToString("|", transform = Pattern::quote), RegexOption.IGNORE_CASE)
}
