package net.yusukezzz.ssmtc.ui.timeline

import android.net.Uri
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.databinding.MediaVideoBinding
import net.yusukezzz.ssmtc.databinding.OpenGraphBinding
import net.yusukezzz.ssmtc.databinding.TweetItemBinding
import net.yusukezzz.ssmtc.ui.misc.AspectRatioImageView
import net.yusukezzz.ssmtc.ui.misc.ThumbnailTileLayout
import net.yusukezzz.ssmtc.util.TextUtil
import net.yusukezzz.ssmtc.util.beVisibleIf
import net.yusukezzz.ssmtc.util.children
import net.yusukezzz.ssmtc.util.getCompatColor
import net.yusukezzz.ssmtc.util.gone
import net.yusukezzz.ssmtc.util.inflate
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import net.yusukezzz.ssmtc.util.resolveAttributeId
import net.yusukezzz.ssmtc.util.visible
import java.text.DecimalFormat

class TimelineAdapter(
    val listener: TweetBinder.TweetItemListener,
    private val ogClient: OpenGraphService
) : RecyclerView.Adapter<TimelineAdapter.TweetViewHolder>() {

    inner class TweetViewHolder(val binding: TweetItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    private var timeline: List<Tweet> = listOf()

    private fun bind(holder: TweetViewHolder, tweet: Tweet) =
        TweetBinder(holder.binding, listener, ogClient).bind(tweet)

    private fun cleanup(holder: TweetViewHolder) {
        holder.binding.run {
            PicassoUtil.cancel(tweetUserImage)
            openGraph.root.reset()
            thumbnailTile.children { PicassoUtil.cancel(it) }
            quoteThumbnailTile.children { PicassoUtil.cancel(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetViewHolder {
        val binding = TweetItemBinding.inflate(LayoutInflater.from(parent.context))
        return TweetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TweetViewHolder, pos: Int) =
        bind(holder, timeline[pos])

    override fun onViewRecycled(holder: TweetViewHolder) =
        cleanup(holder)

    override fun getItemCount(): Int = timeline.size
    override fun getItemId(pos: Int): Long = timeline[pos].id

    fun getAll(): List<Tweet> = timeline.toList()
    fun clear() {
        timeline = listOf()
    }

    fun set(tweets: List<Tweet>) {
        clear()
        add(tweets)
    }

    fun add(tweets: List<Tweet>) {
        timeline = timeline + tweets
        notifyDataSetChanged()
    }
}

class TweetBinder(
    private val binding: TweetItemBinding,
    val listener: TweetItemListener,
    val ogClient: OpenGraphService
) {
    companion object {
        private val numberFormatter = DecimalFormat("#,###,###")
    }

    private val openGraph = OpenGraphBinding.bind(binding.openGraph.root)
    private val context = binding.root.context
    private val resources = binding.root.resources

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

    fun bind(tweet: Tweet) {
        when {
            tweet.isRetweet -> bindRetweeted(tweet)
            tweet.isRetweetWithQuoted -> bindQuoted(tweet)
            else -> bindTweet(tweet)
        }
    }

    private fun bindTweet(tweet: Tweet, removeQuote: Boolean = false) {
        binding.tweetRetweetedContainer.gone()
        binding.quoteContainer.gone()
        openGraph.root.gone()
        binding.thumbnailTile.removeAllViews()
        binding.quoteThumbnailTile.removeAllViews()

        var removeUrl = ""
        if (tweet.hasVideo) {
            handleVideo(tweet.videos.first(), binding.thumbnailTile)
        } else if (tweet.hasPhoto) {
            handlePhoto(tweet.photos, binding.thumbnailTile)
        } else if (!removeQuote && tweet.entities.urls.isNotEmpty()) {
            val urls = tweet.entities.urls
            // ignore host only url
            urls.firstOrNull { Uri.parse(it.expanded_url).path!!.isNotEmpty() }?.let {
                removeUrl = it.url // use open graph view instead of link text
                handleOpenGraph(it.expanded_url)
            }
        }

        val formatted = TextUtil.formattedText(tweet, listener, removeUrl, removeQuote)
        with(tweet) {
            PicassoUtil.userIcon(user, binding.tweetUserImage)
            binding.tweetUserImage.setOnClickListener { listener.onScreenNameClick(user.screenName) }
            binding.tweetUserName.text = user.name
            binding.tweetUserScreenName.text = "@" + user.screenName
            binding.tweetUserProtectedIcon.beVisibleIf(user.isProtected)
            binding.tweetUserVerifiedIcon.beVisibleIf(user.isVerified)
            binding.tweetDate.text =
                DateUtils.getRelativeTimeSpanString(created_at.toEpochSecond() * 1000L)
            binding.tweetText.text = formatted
            binding.tweetText.movementMethod = LinkMovementMethod.getInstance()
            binding.tweetText.beVisibleIf(formatted.isNotEmpty())
        }
        handleReaction(tweet)
    }

    private fun bindRetweeted(tweet: Tweet) {
        bindTweet(tweet.retweeted_status!!)
        binding.tweetRetweetedMessage.text =
            tweet.user.name + resources.getString(R.string.retweeted_by)
        binding.tweetRetweetedContainer.visible()
    }

    private fun bindQuoted(tweet: Tweet) {
        bindTweet(tweet, true)
        val quoted = tweet.quoted_status!!
        binding.quoteText.text = TextUtil.formattedText(quoted, listener)
        binding.quoteText.movementMethod = LinkMovementMethod.getInstance()
        binding.quoteUserName.text = quoted.user.name
        binding.quoteUserScreenName.text = "@" + quoted.user.screenName
        if (quoted.hasVideo) handleVideo(quoted.videos.first(), binding.quoteThumbnailTile)
        if (quoted.hasPhoto) handlePhoto(quoted.photos, binding.quoteThumbnailTile)
        binding.quoteContainer.visible()
    }

    private fun handleReaction(tweet: Tweet) {
        binding.tweetRetweetCount.text = ""
        binding.tweetLikeCount.text = ""

        binding.icTwitterReply.setOnClickListener { listener.onReplyClick(tweet) }

        if (0 < tweet.retweet_count) {
            binding.tweetRetweetCount.text =
                numberFormatter.format(tweet.retweet_count)
        }
        if (tweet.user.isProtected) {
            // cant retweet
            binding.icTwitterRetweet.setColorFilter(
                context.getCompatColor(R.color.action_retweet_protected)
            )
            // remove selectable effect
            binding.icTwitterRetweet.setBackgroundResource(0)
            binding.icTwitterRetweet.setOnClickListener { /* do nothing */ }
        } else {
            val retweetColor = if (tweet.retweeted) {
                R.color.action_retweet_on
            } else {
                R.color.action_icon_default
            }
            binding.icTwitterRetweet.setColorFilter(context.getCompatColor(retweetColor))
            binding.icTwitterRetweet.setBackgroundResource(
                context.resolveAttributeId(android.R.attr.selectableItemBackgroundBorderless)
            )
            binding.icTwitterRetweet.setOnClickListener { listener.onRetweetClick(tweet) }
        }

        if (0 < tweet.favorite_count) {
            binding.tweetLikeCount.text = numberFormatter.format(tweet.favorite_count)
        }
        val likeColor = if (tweet.favorited) {
            R.color.action_like_on
        } else {
            R.color.action_icon_default
        }
        binding.icTwitterLike.setColorFilter(context.getCompatColor(likeColor))
        binding.icTwitterLike.setOnClickListener { listener.onLikeClick(tweet) }

        binding.icTweetShare.setOnClickListener { listener.onShareClick(tweet) }
    }

    private fun handlePhoto(photos: List<Media>, tile: ThumbnailTileLayout) {
        photos.forEachIndexed { index, media ->
            val photoView = AspectRatioImageView(context)
            photoView.setBackgroundColor(context.getCompatColor(R.color.darker_grey))
            tile.addView(photoView)
            photoView.setOnClickListener { listener.onImageClick(photos, index) }
            PicassoUtil.thumbnail(media.smallUrl, photoView)
        }
    }

    private fun handleVideo(video: Media, tile: ThumbnailTileLayout) {
        if (video.video_info == null) return

        val mediaVideo: View = binding.root.inflate(R.layout.media_video)
        val b: MediaVideoBinding = MediaVideoBinding.bind(mediaVideo)
        b.icPlayCircle.setImageResource(R.drawable.ic_play_video)
        b.mediaVideoTime.text = if (video.isGif) {
            "GIF"
        } else {
            TextUtil.milliSecToTime(video.video_info.duration_millis)
        }
        tile.addView(b.root)
        val imgView = b.mediaVideoThumbnail
        imgView.setOnClickListener { listener.onVideoClick(video.video_info) }
        PicassoUtil.thumbnail(video.smallUrl, imgView)
    }

    private fun handleOpenGraph(url: String) {
        if (openGraph.root.isLoaded()) {
            openGraph.root.visible()
        } else {
            openGraph.root.setListener(listener)
            ogClient.load(url, openGraph.root)
        }
    }
}
