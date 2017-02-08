package net.yusukezzz.ssmtc.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.Url
import net.yusukezzz.ssmtc.ui.timeline.TweetItemView.TweetItemListener
import org.apache.commons.lang3.StringEscapeUtils

object TextUtil {
    val SCREEN_NAME_PATTERN = Regex("@[a-zA-Z0-9_]+")
    val HASH_TAG_PATTERN = Regex("(?<!c)#\\w+", RegexOption.IGNORE_CASE)

    fun milliSecToTime(millis: Int): String {
        val sec = millis / 1000
        val m = sec % 3600 / 60
        val s = sec % 3600 % 60

        return "%01d:%02d".format(m, s)
    }

    fun formattedText(tweet: Tweet, listener: TweetItemListener, ogUrl: String = "", removeQuote: Boolean = false): CharSequence {
        val entities = tweet.entities
        val decodedText = StringEscapeUtils.unescapeHtml4(tweet.full_text)

        val urls = entities.urls
        val mediaUrls = entities.media.map { it.urlEntity }

        val spannable = SpannableStringBuilder(removeUrls(decodedText, urls, mediaUrls, ogUrl, removeQuote).trim())

        replaceUrlEntities(spannable, urls, listener)
        replaceScreenName(spannable, listener)
        replaceHashTag(spannable, listener)

        return spannable
    }

    private fun removeUrls(str: String, urls: List<Url>, medias: List<Url>, ogUrl: String, removeQuote: Boolean): String {
        fun String.remove(target: String): String = this.replace(target, "")

        // use quoted tweet view
        val quotedUrl = if (removeQuote) urls.last().url else ""
        // use thumbnails
        val lastMediaUrl = medias.lastOrNull()?.url ?: ""

        return str.remove(quotedUrl).remove(lastMediaUrl)
            .remove(ogUrl) // use open graph view
    }

    private fun replaceUrlEntities(spannable: SpannableStringBuilder,
                                   entities: List<Url>,
                                   listener: TweetItemListener) {
        entities.forEachIndexed { i, entity ->
            val start = spannable.indexOf(entity.url)
            val end = start + entity.url.length

            if (start >= 0) {
                spannable.replace(start, end, entity.display_url)
                val displayEnd = start + entity.display_url.length
                val span = object : ClickableSpan() {
                    override fun onClick(widget: View?) {
                        listener.onUrlClick(entity.expanded_url)
                    }
                }

                spannable.setSpan(span, start, displayEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun replaceScreenName(spannable: SpannableStringBuilder,
                                  listener: TweetItemListener) {
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
                               listener: TweetItemListener) {
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
