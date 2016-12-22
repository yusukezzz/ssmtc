package net.yusukezzz.ssmtc.ui.timeline

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.media_video.view.*
import kotlinx.android.synthetic.main.tweet_body.view.*
import kotlinx.android.synthetic.main.tweet_with_media.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.Media
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.data.json.VideoInfo
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import net.yusukezzz.ssmtc.util.picasso.RoundedTransformation
import java.text.DecimalFormat

class TimelineAdapter(val listener: TweetEventListener) : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        const val THUMBNAIL_IMAGE_TAG = "thumbnail_image_tag"
        const val VIEW_TYPE_NO_MEDIA = 0
        const val VIEW_TYPE_HAS_PHOTO_1 = 1
        const val VIEW_TYPE_HAS_PHOTO_2 = 2
        const val VIEW_TYPE_HAS_PHOTO_3 = 3
        const val VIEW_TYPE_HAS_PHOTO_4 = 4
        const val VIEW_TYPE_HAS_VIDEO = 5
        val LIST_VIEW_TYPE_PHOTO: List<Int> = listOf(
            VIEW_TYPE_HAS_PHOTO_1,
            VIEW_TYPE_HAS_PHOTO_2,
            VIEW_TYPE_HAS_PHOTO_3,
            VIEW_TYPE_HAS_PHOTO_4
        )

        private open class TweetViewHolder(view: View,
                                           val listener: TweetEventListener) : ViewHolder(view) {
            private val numberFormatter = DecimalFormat("#,###,###")

            open fun bindTweet(tweet: Tweet) {
                if (tweet.isRetweet) {
                    handleRetweeted(tweet)
                } else if (tweet.isRetweetWithQuoted) {
                    handleQuoted(tweet)
                } else {
                    handleTweet(tweet)
                }
            }

            fun cleanup() {
                PicassoUtil.clean(itemView.tweet_user_image)
                itemView.tweet_media_container.children { PicassoUtil.clean(it) }
            }

            private fun handleTweet(tweet: Tweet, removeQuote: Boolean = false) {
                itemView.tweet_retweeted_container.visibility = View.GONE
                itemView.quote_container.visibility = View.GONE
                val formatted = TextUtil.formattedText(tweet, listener, removeQuote).trim()
                with(tweet) {
                    Picasso.with(itemView.context).load(user.profileImageUrl)
                        .priority(Picasso.Priority.HIGH)
                        .fit().centerCrop()
                        .transform(RoundedTransformation(8))
                        .into(itemView.tweet_user_image)
                    itemView.tweet_user_name.text = user.name
                    itemView.tweet_user_screen_name.text = "@" + user.screenName
                    itemView.tweet_user_protected_icon.beVisibleIf(user.isProtected)
                    itemView.tweet_user_verified_icon.beVisibleIf(user.isVerified)
                    itemView.tweet_date.text = DateUtils.getRelativeTimeSpanString(created_at.toEpochSecond() * 1000L)
                    itemView.tweet_text.text = formatted
                    itemView.tweet_text.movementMethod = LinkMovementMethod.getInstance()
                    itemView.tweet_text.beVisibleIf(formatted.isNotEmpty())
                }
                handleReaction(tweet)
            }

            private fun handleReaction(tweet: Tweet) {
                itemView.tweet_retweet_count.text = ""
                itemView.tweet_like_count.text = ""

                itemView.ic_twitter_reply.setOnClickListener { listener.onReplyClick(tweet) }

                if (0 < tweet.retweet_count) {
                    itemView.tweet_retweet_count.text = numberFormatter.format(tweet.retweet_count)
                }
                if (tweet.user.isProtected) {
                    // cant retweet
                    itemView.ic_twitter_retweet.setColorFilter(itemView.context.getCompatColor(R.color.action_retweet_protected))
                    // remove selectable effect
                    itemView.ic_twitter_retweet.setBackgroundResource(0)
                    itemView.ic_twitter_retweet.setOnClickListener { /* do nothing */ }
                } else {
                    val retweetColor = if (tweet.retweeted) {
                        R.color.action_retweet_on
                    } else {
                        R.color.action_icon_default
                    }
                    itemView.ic_twitter_retweet.setColorFilter(itemView.context.getCompatColor(retweetColor))
                    val attr = TypedValue()
                    itemView.context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, attr, true)
                    itemView.ic_twitter_retweet.setBackgroundResource(attr.resourceId)
                    itemView.ic_twitter_retweet.setOnClickListener { listener.onRetweetClick(tweet) }
                }

                if (0 < tweet.favorite_count) {
                    itemView.tweet_like_count.text = numberFormatter.format(tweet.favorite_count)
                }
                val likeColor = if (tweet.favorited) {
                    R.color.action_like_on
                } else {
                    R.color.action_icon_default
                }
                itemView.ic_twitter_like.setColorFilter(itemView.context.getCompatColor(likeColor))
                itemView.ic_twitter_like.setOnClickListener { listener.onLikeClick(tweet) }
            }

            private fun handleRetweeted(tweet: Tweet) {
                handleTweet(tweet.retweeted_status!!)
                itemView.tweet_retweeted_message.text = tweet.user.name + itemView.resources.getString(R.string.retweeted_by)
                itemView.tweet_retweeted_container.visibility = View.VISIBLE
            }

            private fun handleQuoted(tweet: Tweet) {
                handleTweet(tweet, true)
                val quoted = tweet.quoted_status!!
                itemView.quote_text.text = TextUtil.formattedText(quoted, listener)
                itemView.quote_text.movementMethod = LinkMovementMethod.getInstance()
                itemView.quote_user_name.text = quoted.user.name
                itemView.quote_user_screen_name.text = "@" + quoted.user.screenName
                itemView.quote_container.visibility = View.VISIBLE
            }
        }

        private class TweetWithPhotoViewHolder(view: View,
                                               listener: TweetEventListener) : TweetViewHolder(view, listener) {
            private val mediaViewIdLists: List<List<Int>> = listOf(
                listOf(R.id.media_photo_single),
                listOf(R.id.media_photo_two_1, R.id.media_photo_two_2),
                listOf(R.id.media_photo_three_1, R.id.media_photo_three_2, R.id.media_photo_three_3),
                listOf(R.id.media_photo_four_1, R.id.media_photo_four_2, R.id.media_photo_four_3, R.id.media_photo_four_4)
            )

            override fun bindTweet(tweet: Tweet) {
                super.bindTweet(tweet)
                handlePhoto(tweet.allMedia)
            }

            fun handlePhoto(photos: List<Media>) {
                val num = photos.size
                val viewIds = mediaViewIdLists[num - 1]
                photos.forEachIndexed { i, m ->
                    val imgView = itemView.findViewById(viewIds[i]) as ImageView
                    imgView.setOnClickListener { listener.onImageClick(photos, i) }
                    Picasso.with(itemView.context).load(m.small_url)
                        .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
                        .into(imgView)
                }
            }
        }

        private class TweetWithVideoViewHolder(view: View,
                                               listener: TweetEventListener) : TweetViewHolder(view, listener) {
            override fun bindTweet(tweet: Tweet) {
                super.bindTweet(tweet)
                handleVideo(tweet.allMedia.first())
            }

            fun handleVideo(video: Media) {
                if (null == video.video_info) return

                itemView.ic_play_circle.setImageResource(R.drawable.ic_play_video)
                itemView.media_video_time.text = TextUtil.milliSecToTime(video.video_info.duration_millis)
                val imgView = itemView.media_video_thumbnail
                imgView.setOnClickListener { listener.onVideoClick(video.video_info) }
                Picasso.with(itemView.context).load(video.small_url)
                    .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
                    .into(imgView)
            }
        }
    }

    interface TweetEventListener {
        fun onImageClick(images: List<Media>, pos: Int)
        fun onVideoClick(video: VideoInfo)
        fun onUrlClick(url: String)
        fun onReplyClick(tweet: Tweet)
        fun onLikeClick(tweet: Tweet)
        fun onRetweetClick(tweet: Tweet)
        fun onScreenNameClick(screenName: String)
        fun onHashTagClick(hashTag: String)
    }

    private val timeline: MutableList<Tweet> = mutableListOf()
    private val photoLayoutIds: List<Int> = listOf(
        R.layout.media_photo_single,
        R.layout.media_photo_two,
        R.layout.media_photo_three,
        R.layout.media_photo_four
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tweetView = parent.inflate(R.layout.tweet_with_media)

        return when (viewType) {
            VIEW_TYPE_NO_MEDIA -> {
                TweetViewHolder(tweetView, listener)
            }
            in VIEW_TYPE_HAS_PHOTO_1..VIEW_TYPE_HAS_PHOTO_4 -> {
                val type = viewType - 1
                val container = parent.inflate(photoLayoutIds[type])
                tweetView.tweet_media_container.addView(container)
                TweetWithPhotoViewHolder(tweetView, listener)
            }
            VIEW_TYPE_HAS_VIDEO -> {
                val container = parent.inflate(R.layout.media_video)
                tweetView.tweet_media_container.addView(container)
                TweetWithVideoViewHolder(tweetView, listener)
            }
            else -> throw RuntimeException("unknown view type: " + viewType)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val tw = timeline[position]

        val size = tw.allMedia.size
        if (size > 0) {
            if (tw.hasVideo) {
                return VIEW_TYPE_HAS_VIDEO
            }

            return LIST_VIEW_TYPE_PHOTO[size - 1]
        }

        return VIEW_TYPE_NO_MEDIA
    }

    override fun onViewRecycled(holder: ViewHolder): Unit {
        (holder as TweetViewHolder).cleanup()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int): Unit {
        (holder as TweetViewHolder).bindTweet(timeline[position])
    }

    override fun getItemCount(): Int = timeline.size

    fun get(pos: Int): Tweet = timeline[pos]
    fun clear() = timeline.clear()

    fun set(tweets: List<Tweet>) {
        timeline.clear()
        add(tweets)
    }

    fun add(tweets: List<Tweet>) {
        timeline.addAll(tweets)
        notifyDataSetChanged()
    }
}
