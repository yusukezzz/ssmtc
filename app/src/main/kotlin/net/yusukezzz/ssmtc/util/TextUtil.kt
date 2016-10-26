package net.yusukezzz.ssmtc.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import net.yusukezzz.ssmtc.data.FormattedMedia
import net.yusukezzz.ssmtc.data.FormattedUrl
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.ui.timeline.TimelineAdapter.TimelineEventListener
import org.apache.commons.lang3.StringEscapeUtils

object TextUtil {
    val SCREEN_NAME_PATTERN = Regex("@[a-zA-Z0-9_]+")
    val HASH_TAG_PATTERN = Regex("#\\w+")

    fun milliSecToTime(millis: Int): String {
        val sec = millis / 1000
        val m = sec % 3600 / 60
        val s = sec % 3600 % 60

        return "%01d:%02d".format(m, s)
    }

    fun formattedText(tweet: Tweet, listener: TimelineEventListener, removeQuote: Boolean = false): CharSequence {
        val entities = tweet.entities
        val decodedText = StringEscapeUtils.unescapeHtml4(tweet.text)
        if (null == entities.urls) return decodedText

        val urls = entities.urls.map(::FormattedUrl)
        val medias = entities.media?.map(::FormattedMedia) ?: listOf()
        val combined = (urls + medias).sortedBy { it.start }

        val lastUrl = if (removeQuote) {
            urls
        } else {
            medias
        }.lastOrNull()?.shortUrl ?: ""
        val spannable = SpannableStringBuilder(decodedText.replace(lastUrl, ""))

        replaceUrlEntities(spannable, combined, listener)
        replaceScreenName(spannable, listener)
        replaceHashTag(spannable, listener)

        return spannable
    }

    private fun replaceUrlEntities(spannable: SpannableStringBuilder,
                                   entities: List<FormattedUrl>,
                                   listener: TimelineEventListener) {
        entities.forEachIndexed { i, entity ->
            val start = spannable.indexOf(entity.shortUrl)
            val end = start + entity.shortUrl.length

            if (start >= 0) {
                spannable.replace(start, end, entity.displayUrl)
                val displayEnd = start + entity.displayUrl.length
                val span = object : ClickableSpan() {
                    override fun onClick(widget: View?) {
                        listener.onUrlClick(entity.url)
                    }
                }

                spannable.setSpan(span, start, displayEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun replaceScreenName(spannable: SpannableStringBuilder,
                                  listener: TimelineEventListener) {
        SCREEN_NAME_PATTERN.findAll(spannable).forEach {
            val span = object : ClickableSpan() {
                override fun onClick(widget: View?) {
                    listener.onScreenNameClick(it.value.removePrefix("@"))
                }
            }
            spannable.setSpan(span, it.range.first, it.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun replaceHashTag(spannable: SpannableStringBuilder,
                               listener: TimelineEventListener) {
        HASH_TAG_PATTERN.findAll(spannable).forEach {
            val span = object : ClickableSpan() {
                override fun onClick(widget: View?) {
                    listener.onHashTagClick(it.value)
                }
            }
            spannable.setSpan(span, it.range.first, it.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
