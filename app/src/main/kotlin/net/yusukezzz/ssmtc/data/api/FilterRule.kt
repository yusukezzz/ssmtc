package net.yusukezzz.ssmtc.data.api

import net.yusukezzz.ssmtc.data.api.FilterRule.Showing.ALL
import paperparcel.PaperParcel
import paperparcel.PaperParcelable

@PaperParcel
data class FilterRule(
    val showing: net.yusukezzz.ssmtc.data.api.FilterRule.Showing,
    val includeWords: List<String>,
    val excludeWords: List<String>
) : PaperParcelable {
    companion object {
        fun default(): net.yusukezzz.ssmtc.data.api.FilterRule = net.yusukezzz.ssmtc.data.api.FilterRule(ALL, listOf(), listOf())
        @JvmField val CREATOR = PaperParcelFilterRule.CREATOR
    }

    enum class Showing {
        ALL, ANY_MEDIA, PHOTO, VIDEO
    }

    fun match(tweet: net.yusukezzz.ssmtc.data.api.model.Tweet): Boolean = matchMedia(tweet) && matchText(tweet)

    private fun matchMedia(tweet: net.yusukezzz.ssmtc.data.api.model.Tweet): Boolean = when (showing) {
        net.yusukezzz.ssmtc.data.api.FilterRule.Showing.ALL -> true // include without media
        net.yusukezzz.ssmtc.data.api.FilterRule.Showing.ANY_MEDIA -> tweet.hasPhoto || tweet.hasVideo
        net.yusukezzz.ssmtc.data.api.FilterRule.Showing.PHOTO -> tweet.hasPhoto
        net.yusukezzz.ssmtc.data.api.FilterRule.Showing.VIDEO -> tweet.hasVideo
    }

    private fun matchText(tweet: net.yusukezzz.ssmtc.data.api.model.Tweet): Boolean {
        if (excludeWords.isNotEmpty() && excludeWords.toRegex().containsMatchIn(tweet.full_text)) return false

        if (includeWords.isEmpty()) return true

        if (includeWords.toRegex().containsMatchIn(tweet.full_text)) return true

        return false
    }

    private fun List<String>.toRegex(): Regex = Regex(this.map(java.util.regex.Pattern::quote).joinToString("|"), RegexOption.IGNORE_CASE)
}
