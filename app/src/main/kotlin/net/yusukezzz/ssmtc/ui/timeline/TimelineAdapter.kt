package net.yusukezzz.ssmtc.ui.timeline

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.media_video.view.*
import kotlinx.android.synthetic.main.tweet_body.view.*
import kotlinx.android.synthetic.main.tweet_with_media.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.Media
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.data.json.VideoInfo
import net.yusukezzz.ssmtc.ui.views.AspectRatioImageView
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import net.yusukezzz.ssmtc.util.picasso.RoundedTransformation
import java.text.DecimalFormat

class TimelineAdapter(val listener: TweetEventListener) : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        const val THUMBNAIL_IMAGE_TAG = "thumbnail_image_tag"

        const val VIEW_TYPE_TWEET = 0
        const val VIEW_TYPE_RETWEETED = 1
        const val VIEW_TYPE_QUOTED = 2

        private open class TweetViewHolder(view: View,
                                           val viewType: Int,
                                           val listener: TweetEventListener) : ViewHolder(view) {
            private val numberFormatter = DecimalFormat("#,###,###")

            fun bindTweet(tweet: Tweet) {
                when (viewType) {
                    VIEW_TYPE_TWEET -> handleTweet(tweet)
                    VIEW_TYPE_RETWEETED -> handleRetweeted(tweet)
                    VIEW_TYPE_QUOTED -> handleQuoted(tweet)
                }
            }

            fun cleanup() {
                PicassoUtil.clean(itemView.tweet_user_image)
                itemView.thumbnail_tile.children { PicassoUtil.clean(it) }
                itemView.thumbnail_tile.removeAllViews()
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

                if (tweet.hasVideo) {
                    handleVideo(tweet.videos.first())
                } else if (tweet.hasPhoto) {
                    handlePhoto(tweet.photos)
                }
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

            private fun handlePhoto(photos: List<Media>) {
                photos.forEachIndexed { index, media ->
                    val photoView = AspectRatioImageView(itemView.context)
                    itemView.thumbnail_tile.addView(photoView)
                    photoView.setOnClickListener { listener.onImageClick(photos, index) }
                    Picasso.with(itemView.context).load(media.small_url)
                        .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
                        .into(photoView)
                }
            }

            private fun handleVideo(video: Media) {
                if (video.video_info == null) return

                val container = itemView.thumbnail_tile.inflate(R.layout.media_video)
                container.ic_play_circle.setImageResource(R.drawable.ic_play_video)
                container.media_video_time.text = if (video.isGif) {
                    "GIF"
                } else {
                    TextUtil.milliSecToTime(video.video_info.duration_millis)
                }
                itemView.thumbnail_tile.addView(container)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tweetView = parent.inflate(R.layout.tweet_with_media)

        return TweetViewHolder(tweetView, viewType, listener)
    }

    override fun getItemViewType(position: Int): Int {
        val tw = timeline[position]

        return if (tw.isRetweet) {
            VIEW_TYPE_RETWEETED
        } else if (tw.isRetweetWithQuoted) {
            VIEW_TYPE_QUOTED
        } else {
            VIEW_TYPE_TWEET
        }
    }

    override fun onViewRecycled(holder: ViewHolder): Unit {
        (holder as TweetViewHolder).cleanup()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int): Unit {
        (holder as TweetViewHolder).bindTweet(timeline[position])
    }

    override fun getItemCount(): Int = timeline.size

    fun get(pos: Int): Tweet = timeline[pos]
    fun getAll(): List<Tweet> = timeline.toList()
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
