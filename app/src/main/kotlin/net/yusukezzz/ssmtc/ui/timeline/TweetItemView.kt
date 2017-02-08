package net.yusukezzz.ssmtc.ui.timeline

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.media_video.view.*
import kotlinx.android.synthetic.main.open_graph.view.*
import kotlinx.android.synthetic.main.tweet_item.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.data.og.OpenGraphClient
import net.yusukezzz.ssmtc.ui.misc.AspectRatioImageView
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import java.text.DecimalFormat

class TweetItemView : LinearLayout {
    private val numberFormatter = DecimalFormat("#,###,###")
    private var position: Int = 0
    private lateinit var listener: TweetItemListener
    private lateinit var ogClient: OpenGraphClient

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

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setPosition(pos: Int) {
        this.position = pos
    }

    fun setTweetListener(listener: TweetItemListener) {
        this.listener = listener
    }

    fun setOpenGraphClient(client: OpenGraphClient) {
        this.ogClient = client
    }

    fun bindTweet(tweet: Tweet, removeQuote: Boolean = false) {
        tweet_retweeted_container.gone()
        quote_container.gone()
        open_graph.gone()
        thumbnail_tile.removeAllViews()

        var removeUrl = ""
        if (tweet.hasVideo) {
            handleVideo(tweet.videos.first())
        } else if (tweet.hasPhoto) {
            handlePhoto(tweet.photos)
        } else if (!removeQuote && tweet.entities.urls.isNotEmpty()) {
            val urls = tweet.entities.urls
            // ignore host only url
            urls.filter { Uri.parse(it.expanded_url).path.isNotEmpty() }.firstOrNull()?.let {
                removeUrl = it.url // use open graph view instead of link text
                handleOpenGraph(it.expanded_url)
            }
        }

        val formatted = TextUtil.formattedText(tweet, listener, removeUrl, removeQuote)
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

        // handle more actions
        ic_tweet_share.setOnClickListener { listener.onShareClick(tweet) }
    }

    fun bindRetweeted(tweet: Tweet) {
        bindTweet(tweet.retweeted_status!!)
        tweet_retweeted_message.text = tweet.user.name + resources.getString(R.string.retweeted_by)
        tweet_retweeted_container.visible()
    }

    fun bindQuoted(tweet: Tweet) {
        bindTweet(tweet, true)
        val quoted = tweet.quoted_status!!
        quote_text.text = TextUtil.formattedText(quoted, listener)
        quote_text.movementMethod = LinkMovementMethod.getInstance()
        quote_user_name.text = quoted.user.name
        quote_user_screen_name.text = "@" + quoted.user.screenName
        quote_container.visible()
    }

    fun cleanup() {
        PicassoUtil.cancel(tweet_user_image)
        PicassoUtil.cancel(open_graph.og_image)
        thumbnail_tile.children { PicassoUtil.cancel(it) }
    }

    private fun handlePhoto(photos: List<Media>) {
        photos.forEachIndexed { index, media ->
            val photoView = AspectRatioImageView(context)
            photoView.setBackgroundColor(context.getCompatColor(R.color.darker_grey))
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

    private fun handleOpenGraph(url: String) {
        val og = ogClient.load(url, position)
        if (og == null) {
            og_contents.gone()
            og_loading.visible()
        } else {
            open_graph.og_title.text = og.title
            open_graph.og_host.text = Uri.parse(og.url).host
            PicassoUtil.opengraph(og.image, open_graph.og_image)
            open_graph.setOnClickListener {
                listener.onUrlClick(og.url)
            }
            og_loading.gone()
            og_contents.visible()
        }
        open_graph.visible()
    }
}
