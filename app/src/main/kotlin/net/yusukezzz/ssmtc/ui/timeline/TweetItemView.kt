package net.yusukezzz.ssmtc.ui.timeline

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.media_video.view.*
import kotlinx.android.synthetic.main.open_graph.view.*
import kotlinx.android.synthetic.main.tweet_item.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.ui.misc.AspectRatioImageView
import net.yusukezzz.ssmtc.ui.misc.ThumbnailTileLayout
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import java.text.DecimalFormat

class TweetItemView : LinearLayout {
    companion object {
        private val numberFormatter = DecimalFormat("#,###,###")
    }
    private lateinit var listener: TweetItemListener
    private lateinit var ogClient: OpenGraphService
    private val mediaVideo: View by lazy { this.inflate(R.layout.media_video) }

    interface TweetItemListener {
        fun onImageClick(images: List<Media>, pos: Int)
        fun onVideoClick(video: VideoInfo)
        fun onReplyClick(tweet: Tweet)
        fun onLikeClick(tweet: Tweet)
        fun onRetweetClick(tweet: Tweet)
        fun onShareClick(tweet: Tweet)

        // for TextUtil
        fun onUrlClick(url: String)
        fun onScreenNameClick(screenName: String)
        fun onHashTagClick(hashTag: String)
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(context, attrs, defStyle)

    fun setTweetListener(listener: TweetItemListener) {
        this.listener = listener
    }

    fun setOpenGraphClient(client: OpenGraphService) {
        this.ogClient = client
    }

    fun bind(tweet: Tweet) = when {
        tweet.isRetweet -> bindRetweeted(tweet)
        tweet.isRetweetWithQuoted -> bindQuoted(tweet)
        else -> bindTweet(tweet)
    }

    private fun bindTweet(tweet: Tweet, removeQuote: Boolean = false) {
        tweet_retweeted_container.gone()
        quote_container.gone()
        open_graph.gone()
        thumbnail_tile.removeAllViews()
        quote_thumbnail_tile.removeAllViews()

        var removeUrl = ""
        if (tweet.hasVideo) {
            handleVideo(tweet.videos.first(), thumbnail_tile)
        } else if (tweet.hasPhoto) {
            handlePhoto(tweet.photos, thumbnail_tile)
        } else if (!removeQuote && tweet.entities.urls.isNotEmpty()) {
            val urls = tweet.entities.urls
            // ignore host only url
            urls.firstOrNull { Uri.parse(it.expanded_url).path.isNotEmpty() }?.let {
                removeUrl = it.url // use open graph view instead of link text
                handleOpenGraph(it.expanded_url)
            }
        }

        val formatted = TextUtil.formattedText(tweet, listener, removeUrl, removeQuote)
        with(tweet) {
            PicassoUtil.userIcon(user, tweet_user_image)
            tweet_user_image.setOnClickListener { listener.onScreenNameClick(user.screenName) }
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
    }

    private fun bindRetweeted(tweet: Tweet) {
        bindTweet(tweet.retweeted_status!!)
        tweet_retweeted_message.text = tweet.user.name + resources.getString(R.string.retweeted_by)
        tweet_retweeted_container.visible()
    }

    private fun bindQuoted(tweet: Tweet) {
        bindTweet(tweet, true)
        val quoted = tweet.quoted_status!!
        quote_text.text = TextUtil.formattedText(quoted, listener)
        quote_text.movementMethod = LinkMovementMethod.getInstance()
        quote_user_name.text = quoted.user.name
        quote_user_screen_name.text = "@" + quoted.user.screenName
        if (quoted.hasVideo) handleVideo(quoted.videos.first(), quote_thumbnail_tile)
        if (quoted.hasPhoto) handlePhoto(quoted.photos, quote_thumbnail_tile)
        quote_container.visible()
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
            ic_twitter_retweet.setBackgroundResource(context.resolveAttributeId(android.R.attr.selectableItemBackgroundBorderless))
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

        ic_tweet_share.setOnClickListener { listener.onShareClick(tweet) }
    }

    fun cleanup() {
        PicassoUtil.cancel(tweet_user_image)
        open_graph.reset()
        thumbnail_tile.children { PicassoUtil.cancel(it) }
        quote_thumbnail_tile.children { PicassoUtil.cancel(it) }
    }

    private fun handlePhoto(photos: List<Media>, tile: ThumbnailTileLayout) {
        photos.forEachIndexed { index, media ->
            val photoView = AspectRatioImageView(context)
            photoView.setBackgroundColor(context.getCompatColor(R.color.darker_grey))
            tile.addView(photoView)
            photoView.setOnClickListener { listener.onImageClick(photos, index) }
            PicassoUtil.thumbnail(media.small_url, photoView)
        }
    }

    private fun handleVideo(video: Media, tile: ThumbnailTileLayout) {
        if (video.video_info == null) return

        mediaVideo.ic_play_circle.setImageResource(R.drawable.ic_play_video)
        mediaVideo.media_video_time.text = if (video.isGif) {
            "GIF"
        } else {
            TextUtil.milliSecToTime(video.video_info.duration_millis)
        }
        tile.addView(mediaVideo)
        val imgView = media_video_thumbnail
        imgView.setOnClickListener { listener.onVideoClick(video.video_info) }
        PicassoUtil.thumbnail(video.small_url, imgView)
    }

    private fun handleOpenGraph(url: String) {
        if (open_graph.isLoaded()) {
            open_graph.visible()
        } else {
            open_graph.setListener(listener)
            ogClient.load(url, open_graph)
        }
    }
}
