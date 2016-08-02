package net.yusukezzz.ssmtc.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import net.yusukezzz.ssmtc.data.FormattedMedia
import net.yusukezzz.ssmtc.data.FormattedUrl
import net.yusukezzz.ssmtc.data.json.Media
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.screens.timeline.TimelineAdapter.TimelineEventListener
import org.apache.commons.lang3.StringEscapeUtils

class TextUtil {
    companion object {
        val SCREEN_NAME_PATTERN = "@[a-zA-Z0-9_]+"
        val HASH_TAG_PATTERN = "#\\w+"

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

            val urls = entities.urls.map { FormattedUrl(it) }
            val medias = entities.media?.map { FormattedMedia(it) }
            val spannable = SpannableStringBuilder(decodedText)

            val combined = if (null == medias) urls else (urls + medias).sortedBy { it.start }
            val lastPhoto = medias?.filter { Media.TYPE_PHOTO == it.type }?.last()

            replaceUrlEntities(spannable, combined, lastPhoto, removeQuote, listener)
            replaceScreenName(spannable, listener)
            replaceHashTag(spannable, listener)

            return spannable
        }

        // from twitter-kit-android TweetTextLinkifier class
        private fun replaceUrlEntities(spannable: SpannableStringBuilder,
                                       entities: List<FormattedUrl>,
                                       lastPhoto: FormattedMedia?,
                                       removeQuote: Boolean,
                                       listener: TimelineEventListener) {
            var offset = 0
            var len: Int
            var start: Int
            var end: Int

            entities.forEachIndexed { index, entity ->
                start = entity.start - offset
                end = entity.end - offset
                if (start >= 0 && end <= spannable.length) {
                    if (null != lastPhoto && lastPhoto.start == entity.start) {
                        spannable.replace(start, end, "")
                        len = end - start
                        end -= len
                        offset += len
                    } else if (removeQuote && index == entities.lastIndex) {
                        spannable.replace(start, end, "")
                        len = end - start
                        end -= len
                        offset += len
                    } else if (entity.displayUrl.isNotEmpty()) {
                        spannable.replace(start, end, entity.displayUrl)
                        len = end - (start + entity.displayUrl.length)
                        end -= len
                        offset += len

                        val span = object: ClickableSpan() {
                            override fun onClick(widget: View?) {
                                listener.onUrlClick(entity.url)
                            }
                        }

                        spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        }

        private fun replaceScreenName(spannable: SpannableStringBuilder,
                                      listener: TimelineEventListener) {
            Regex(SCREEN_NAME_PATTERN).findAll(spannable).forEach {
                val span = object: ClickableSpan() {
                    override fun onClick(widget: View?) {
                        listener.onScreenNameClick(it.value)
                    }
                }
                spannable.setSpan(span, it.range.first, it.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        private fun replaceHashTag(spannable: SpannableStringBuilder,
                                   listener: TimelineEventListener) {
            Regex(HASH_TAG_PATTERN).findAll(spannable).forEach {
                val span = object: ClickableSpan() {
                    override fun onClick(widget: View?) {
                        listener.onHashTagClick(it.value)
                    }
                }
                spannable.setSpan(span, it.range.first, it.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
