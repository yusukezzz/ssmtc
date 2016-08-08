package net.yusukezzz.ssmtc.screens.timeline

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.timeline_list.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.data.json.VideoInfo
import net.yusukezzz.ssmtc.screens.media.photo.gallery.GalleryActivity
import net.yusukezzz.ssmtc.screens.media.video.VideoPlayerActivity
import net.yusukezzz.ssmtc.screens.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.util.getVectorDrawable
import net.yusukezzz.ssmtc.util.toBitmap
import net.yusukezzz.ssmtc.util.toast

class TimelineFragment: Fragment(),
    TimelineContract.View,
    SwipeRefreshLayout.OnRefreshListener,
    EndlessRecyclerOnScrollListener.ScrollListener,
    TimelineAdapter.TimelineEventListener {

    companion object {
        fun newInstance(listener: TimelineFragmentListener) = TimelineFragment().apply { setListener(listener) }
    }

    interface TimelineFragmentListener {
        fun onTimelineReady()
    }

    private lateinit var presenter: TimelineContract.Presenter
    private lateinit var listener: TimelineFragmentListener
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private val timelineAdapter: TimelineAdapter by lazy { timeline_list.adapter as TimelineAdapter }
    private val act: TimelineActivity by lazy { activity as TimelineActivity }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.timeline_list, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        endlessScrollListener = EndlessRecyclerOnScrollListener(activity, layoutManager)
        endlessScrollListener.setLoadMoreListener(this)
        timeline_list.setHasFixedSize(true)
        timeline_list.layoutManager = layoutManager
        timeline_list.addOnScrollListener(endlessScrollListener)
        timeline_list.adapter = TimelineAdapter(this)

        swipe_refresh.setOnRefreshListener(this)
        swipe_refresh.setColorSchemeResources(R.color.green, R.color.red, R.color.blue, R.color.yellow)

        activity.findViewById(R.id.toolbar_title).setOnClickListener {
            timeline_list.scrollToPosition(0)
        }

        tweet_btn.setOnClickListener {
            val i = Intent(activity, StatusUpdateActivity::class.java)
            startActivity(i)
        }

        listener.onTimelineReady()
    }

    override fun onDestroyView() {
        timeline_list.clearOnScrollListeners()
        timeline_list.adapter = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        // update tweet time
        timelineAdapter.notifyDataSetChanged()
    }

    override fun onRefresh() = presenter.loadNewerTweets()

    override fun setPresenter(presenter: TimelineContract.Presenter) {
        this.presenter = presenter
    }

    fun setListener(listener: TimelineFragmentListener) {
        this.listener = listener
    }

    override fun getLatestTweetId(): Long? = timelineAdapter.first()?.id

    override fun getLastTweetId(): Long? = timelineAdapter.last()?.id

    override fun addHeadTweets(tweets: List<Tweet>) {
        timelineAdapter.set(tweets)
        swipe_refresh.isRefreshing = false
    }

    override fun addTailTweets(tweets: List<Tweet>) {
        timelineAdapter.add(tweets)
        println("tweets pushed")
        // TODO: more loading progress off
    }

    override fun onLoadMore(currentPage: Int) {
        println("onLoadMore")
        presenter.loadOlderTweets()
        // TODO: more loading progress on
    }

    override fun initialize() {
        endlessScrollListener.reset()
        timelineAdapter.clear()
        swipe_refresh.post({
            swipe_refresh.isRefreshing = true
            onRefresh()
        })
        timeline_list.scrollToPosition(0)
    }

    override fun onUrlClick(url: String) {
        val urlIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, url)
        val pending = PendingIntent.getActivity(activity, 0, urlIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val backIcon = context.getVectorDrawable(R.drawable.ic_arrow_back, android.R.color.white).toBitmap()
        val shareIcon = context.getVectorDrawable(R.drawable.ic_share, android.R.color.white).toBitmap()

        val chromeIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary))
            .setCloseButtonIcon(backIcon)
            .setActionButton(shareIcon, "Share", pending)
            .build()

        chromeIntent.launchUrl(activity, Uri.parse(url))
    }

    override fun onImageClick(images: List<String>, pos: Int) {
        startActivity(GalleryActivity.newIntent(activity, images, pos))
    }

    override fun onVideoClick(video: VideoInfo) {
        startActivity(VideoPlayerActivity.newIntent(activity, video))
    }

    override fun onReplyClick(tweet: Tweet) {
        val i = StatusUpdateActivity.newIntent(activity, tweet.id, tweet.user.screenName)
        startActivity(i)
    }

    override fun onLikeClick(tweet: Tweet) {
        if (tweet.favorited) {
            presenter.unlike(tweet)
        } else {
            presenter.like(tweet)
        }
    }

    override fun onRetweetClick(tweet: Tweet) {
        if (tweet.retweeted) {
            presenter.unretweet(tweet)
        } else {
            presenter.retweet(tweet)
        }
    }

    override fun onScreenNameClick(screenName: String) {
        act.onTimelineSelected(TimelineParameter.user(screenName))
    }

    override fun onHashTagClick(hashTag: String) {
        act.onTimelineSelected(TimelineParameter.search(hashTag))
    }

    override fun updateReactedTweet() {
        timelineAdapter.notifyDataSetChanged()
    }

    override fun handleError(error: Throwable) {
        println(error)
        toast(error.message)
        swipe_refresh.isRefreshing = false
    }
}
