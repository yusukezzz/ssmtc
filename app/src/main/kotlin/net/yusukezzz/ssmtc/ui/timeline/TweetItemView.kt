package net.yusukezzz.ssmtc.ui.timeline

import android.content.Context
import android.support.v7.widget.CardView
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.media_video.view.*
import kotlinx.android.synthetic.main.tweet_item.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.Media
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.data.json.VideoInfo
import net.yusukezzz.ssmtc.ui.views.AspectRatioImageView
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import java.text.DecimalFormat

class TweetItemView : CardView {
    private val numberFormatter = DecimalFormat("#,###,###")
    private val picasso: Picasso by lazy { Picasso.with(context) }
    private lateinit var listener: TweetItemListener

    interface TweetItemListener {
        fun onImageClick(images: List<Media>, pos: Int)
        fun onVideoClick(video: VideoInfo)
        fun onReplyClick(tweet: Tweet)
        fun onLikeClick(tweet: Tweet)
        fun onRetweetClick(tweet: Tweet)

        // for TextUtil
        fun onUrlClick(url: String)
        fun onScreenNameClick(screenName: String)
        fun onHashTagClick(hashTag: String)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setTweetListener(listener: TweetItemListener) {
        this.listener = listener
    }

    fun bindTweet(tweet: Tweet, removeQuote: Boolean = false) {
        tweet_retweeted_container.visibility = View.GONE
        quote_container.visibility = View.GONE
        val formatted = TextUtil.formattedText(tweet, listener, removeQuote).trim()
        with(tweet) {
            PicassoUtil.userIcon(user, tweet_user_image)
            tweet_user_name.text = user.name
            tweet_user_screen_name.text = "@" + user.screenName
            tweet_user_protected_icon.beVisibleIf(user.isProtected)
            tweet_user_verified_icon.beVisibleIf(user.isVerified)
            tweet_date.text = DateUtils.getRelativeTimeSpanString(created_at.toEpochSecond() * 1000L)
            tweet_text.text = formatted
            tweet_text.movementMethod = LinkMovementMethod.getInstance()
            tweet_text.beVisibleIf(formatted.isNotEmpty())
        }
        handleReaction(tweet)

        if (tweet.hasVideo) {
            handleVideo(tweet.videos.first())
        } else if (tweet.hasPhoto) {
            handlePhoto(tweet.photos)
        }
    }

    private fun handleReaction(tweet: Tweet) {
        tweet_retweet_count.text = ""
        tweet_like_count.text = ""

        ic_twitter_reply.setOnClickListener { listener.onReplyClick(tweet) }

        if (0 < tweet.retweet_count) {
            tweet_retweet_count.text = numberFormatter.format(tweet.retweet_count)
        }
        if (tweet.user.isProtected) {
            // cant retweet
            ic_twitter_retweet.setColorFilter(context.getCompatColor(R.color.action_retweet_protected))
            // remove selectable effect
            ic_twitter_retweet.setBackgroundResource(0)
            ic_twitter_retweet.setOnClickListener { /* do nothing */ }
        } else {
            val retweetColor = if (tweet.retweeted) {
                R.color.action_retweet_on
            } else {
                R.color.action_icon_default
            }
            ic_twitter_retweet.setColorFilter(context.getCompatColor(retweetColor))
            val attr = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, attr, true)
            ic_twitter_retweet.setBackgroundResource(attr.resourceId)
            ic_twitter_retweet.setOnClickListener { listener.onRetweetClick(tweet) }
        }

        if (0 < tweet.favorite_count) {
            tweet_like_count.text = numberFormatter.format(tweet.favorite_count)
        }
        val likeColor = if (tweet.favorited) {
            R.color.action_like_on
        } else {
            R.color.action_icon_default
        }
        ic_twitter_like.setColorFilter(context.getCompatColor(likeColor))
        ic_twitter_like.setOnClickListener { listener.onLikeClick(tweet) }
    }

    fun bindRetweeted(tweet: Tweet) {
        bindTweet(tweet.retweeted_status!!)
        tweet_retweeted_message.text = tweet.user.name + resources.getString(R.string.retweeted_by)
        tweet_retweeted_container.visibility = View.VISIBLE
    }

    fun bindQuoted(tweet: Tweet) {
        bindTweet(tweet, true)
        val quoted = tweet.quoted_status!!
        quote_text.text = TextUtil.formattedText(quoted, listener)
        quote_text.movementMethod = LinkMovementMethod.getInstance()
        quote_user_name.text = quoted.user.name
        quote_user_screen_name.text = "@" + quoted.user.screenName
        quote_container.visibility = View.VISIBLE
    }

    fun cleanup() {
        PicassoUtil.clean(tweet_user_image)
        thumbnail_tile.children { PicassoUtil.clean(it) }
        thumbnail_tile.removeAllViews()
    }

    private fun handlePhoto(photos: List<Media>) {
        photos.forEachIndexed { index, media ->
            val photoView = AspectRatioImageView(context)
            thumbnail_tile.addView(photoView)
            photoView.setOnClickListener { listener.onImageClick(photos, index) }
            PicassoUtil.thumbnail(media.small_url, photoView)
        }
    }

    private fun handleVideo(video: Media) {
        if (video.video_info == null) return

        val container = thumbnail_tile.inflate(R.layout.media_video)
        container.ic_play_circle.setImageResource(R.drawable.ic_play_video)
        container.media_video_time.text = if (video.isGif) {
            "GIF"
        } else {
            TextUtil.milliSecToTime(video.video_info.duration_millis)
        }
        thumbnail_tile.addView(container)
        val imgView = media_video_thumbnail
        imgView.setOnClickListener { listener.onVideoClick(video.video_info) }
        PicassoUtil.thumbnail(video.small_url, imgView)
    }
}
