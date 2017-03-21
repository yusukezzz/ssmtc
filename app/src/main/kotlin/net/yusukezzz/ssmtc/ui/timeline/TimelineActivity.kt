package net.yusukezzz.ssmtc.ui.timeline

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.base_layout.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.timeline_layout.*
import kotlinx.android.synthetic.main.timeline_list.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.Twitter
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.data.api.model.TwList
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.data.og.OpenGraphClient
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.media.photo.gallery.GalleryActivity
import net.yusukezzz.ssmtc.ui.media.video.VideoPlayerActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.ui.timeline.dialogs.*
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import java.io.File
import javax.inject.Inject

class TimelineActivity: AppCompatActivity(),
    TimelineContract.View,
    SwipeRefreshLayout.OnRefreshListener,
    EndlessRecyclerOnScrollListener.ScrollListener,
    TweetItemView.TweetItemListener,
    NavigationView.OnNavigationItemSelectedListener,
    TimelineSettingDialog.TimelineSettingListener,
    BaseDialogFragment.TimelineSelectListener {

    companion object {
        const val STATE_OLDEST_TWEET_ID = "state_oldest_tweet_id"
        const val STATE_RECYCLER_VIEW = "state_recycler_view"
    }

    private lateinit var presenter: TimelineContract.Presenter
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private var listsLoading: AlertDialog? = null
    private val timelineAdapter: TimelineAdapter by lazy { timeline_list.adapter as TimelineAdapter }
    private val lastTimelineFile by lazy { File(applicationContext.cacheDir, "last_timeline.json") }

    // Oldest tweet id on current timeline (use for next paging request)
    private var lastTweetId: Long? = null

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: Twitter

    @Inject
    lateinit var ogClient: OpenGraphClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.inject(this)

        if (prefs.getCurrentAccount() == null) {
            launchAuthorizeActivity()
            finish()
            return
        }

        setContentView(R.layout.timeline_layout)
        main_contents.setView(R.layout.timeline_list)
        setSupportActionBar(toolbar)

        setupDrawerView()
        setupTimelineView()
        loadAccount()

        if (savedInstanceState != null && lastTimelineFile.exists()) {
            restoreTimeline(savedInstanceState)
        } else {
            // initial load
            switchTimeline(prefs.currentTimeline)
        }
    }

    override fun onStart() {
        super.onStart()

        // warmup chrome custom tabs
        CustomTabsClient.connectAndInitialize(this, CustomTabsClient.getPackageName(this, null))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // save last tweets
        val json = gson.toJson(timelineAdapter.getAll())
        lastTimelineFile.writeText(json)
        // save last scroll position
        outState.putParcelable(STATE_RECYCLER_VIEW, timeline_list.layoutManager.onSaveInstanceState())
        // save oldest tweet id
        lastTweetId?.let { outState.putLong(STATE_OLDEST_TWEET_ID, it) }
    }

    private fun restoreTimeline(state: Bundle) {
        toolbar_title.text = prefs.currentTimeline.title
        presenter = TimelinePresenter(this, twitter, prefs.currentTimeline)
        updateTimelineMenu()

        // load tweets from file
        val json = lastTimelineFile.readText()
        val tweets: List<Tweet> = gson.fromJson(json, object : TypeToken<List<Tweet>>() {}.type)
        val timelineState = state.getParcelable<Parcelable>(STATE_RECYCLER_VIEW)
        timelineAdapter.set(tweets)
        timeline_list.layoutManager.onRestoreInstanceState(timelineState)
        val oldestTweetId = state.getLong(STATE_OLDEST_TWEET_ID)
        if (oldestTweetId != 0L) {
            lastTweetId = oldestTweetId
        }

        stopLoading()
    }

    private fun setupDrawerView() {
        val drawerToggle = object : ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                // always back to timeline navigation
                showTimelineNavigation()
            }
        }
        drawer.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun setupTimelineView() {
        timeline_list.adapter = TimelineAdapter(this, ogClient).apply { setHasStableIds(true) }
        timeline_list.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        timeline_list.layoutManager = layoutManager
        val decoration = DividerItemDecoration(this, layoutManager.orientation)
        decoration.setDrawable(getCompatDrawable(R.drawable.timeline_divider))
        timeline_list.addItemDecoration(decoration)

        endlessScrollListener = EndlessRecyclerOnScrollListener(this, layoutManager)
        endlessScrollListener.setLoadMoreListener(this)
        timeline_list.addOnScrollListener(endlessScrollListener)

        swipe_refresh.setOnRefreshListener(this)
        swipe_refresh.setColorSchemeResources(R.color.green, R.color.red, R.color.blue, R.color.yellow)

        findViewById(R.id.toolbar_title).setOnClickListener {
            timeline_list.scrollToPosition(0)
        }

        tweet_btn.setOnClickListener {
            startActivity(StatusUpdateActivity.newIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        // update relative tweet time
        timelineAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_timeline, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove_timeline -> {
                prefs.removeCurrentTimeline()
                switchTimeline(prefs.currentTimeline)
            }
            R.id.setting_timeline -> {
                showTimelineSettingDialog()
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer.closeDrawer(GravityCompat.START)

        if (item.isChecked) {
            return true
        }

        when (item.groupId) {
            R.id.menu_account -> return handleAccountNavigation(item)
            R.id.menu_timeline -> return handleTimelineNavigation(item)
            R.id.menu_manage -> return handleManageNavigation(item)
            else -> return false
        }
    }

    fun handleAccountNavigation(item: MenuItem): Boolean {
        val accounts = (prefs.accounts - prefs.getCurrentAccount()!!)
        prefs.currentUserId = accounts[item.order].user.id
        loadAccount()
        showTimelineNavigation()
        switchTimeline(prefs.currentTimeline)

        return false
    }

    fun handleTimelineNavigation(item: MenuItem): Boolean {
        prefs.currentTimelineIndex = item.order
        switchTimeline(prefs.currentTimeline)

        return true
    }

    fun handleManageNavigation(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_account_add -> launchAuthorizeActivity()
            R.id.nav_account_remove -> confirmRemoveAccount()
            R.id.nav_timeline_selector -> showTimelineSelector()
            else -> toast("unknown menu item: ${item.title}")
        }

        return false
    }

    fun launchAuthorizeActivity() = startActivity(Intent(this, AuthorizeActivity::class.java))

    fun loadAccount() {
        val account = prefs.getCurrentAccount()!!
        twitter.setTokens(account.accessToken, account.secretToken)

        val headerView = nav_view.getHeaderView(0)
        val profileImage = headerView.findViewById(R.id.profile_image) as ImageView
        val screenName = headerView.findViewById(R.id.screen_name) as TextView
        val accountSelectBtn = headerView.findViewById(R.id.btn_account_selector) as ImageView
        PicassoUtil.userIcon(account.user, profileImage)
        screenName.text = account.user.screenName

        accountSelectBtn.setOnClickListener {
            toggleNavigationContents()
        }
    }

    /**
     * toggle timeline and account navigation menu
     */
    fun toggleNavigationContents() {
        val isAccountNav = (nav_view.menu.findItem(R.id.nav_account_add) != null)
        if (isAccountNav) {
            showTimelineNavigation()
        } else {
            showAccountNavigation()
        }
    }

    fun showTimelineNavigation() {
        nav_view.menu.clear()
        btn_account_selector.setImageResource(R.drawable.ic_arrow_drop_down)
        nav_view.inflateMenu(R.menu.menu_drawer_timeline)
        updateTimelineMenu()
    }

    fun showAccountNavigation() {
        nav_view.menu.clear()
        btn_account_selector.setImageResource(R.drawable.ic_arrow_drop_up)
        nav_view.inflateMenu(R.menu.menu_drawer_account)
        (prefs.accounts - prefs.getCurrentAccount()!!).forEachIndexed { i, account ->
            nav_view.menu.add(R.id.menu_account, Menu.NONE, i, account.user.screenName)
        }
    }

    fun confirmRemoveAccount() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_account_remove_message)
            .setPositiveButton(R.string.confirm_account_remove_ok, { _, _ -> removeAccount() })
            .setNegativeButton(R.string.confirm_account_remove_cancel, { _, _ -> /* do nothing */ })
            .show()
    }

    fun removeAccount() {
        prefs.removeCurrentAccount()
        if (prefs.getCurrentAccount() == null) {
            launchAuthorizeActivity()
        } else {
            loadAccount()
            switchTimeline(prefs.currentTimeline)
        }
    }

    fun updateTimelineMenu() {
        nav_view.menu.removeGroup(R.id.menu_timeline)
        prefs.getCurrentAccount()!!.timelines.forEachIndexed { index, timeline ->
            nav_view.menu.add(R.id.menu_timeline, Menu.NONE, index, timeline.title)
                .setCheckable(true)
                .setIcon(timelineIcon(timeline.type))
                .isChecked = (index == prefs.currentTimelineIndex)
        }
    }

    fun timelineIcon(type: Int): Int = when (type) {
        TimelineParameter.TYPE_HOME -> R.drawable.ic_timeline_home
        TimelineParameter.TYPE_MENTIONS -> R.drawable.ic_timeline_mention
        TimelineParameter.TYPE_LISTS -> R.drawable.ic_timeline_list
        TimelineParameter.TYPE_SEARCH -> R.drawable.ic_timeline_search
        TimelineParameter.TYPE_USER -> R.drawable.ic_timeline_user
        else -> R.drawable.ic_timeline_home
    }

    fun switchTimeline(timeline: TimelineParameter) {
        toolbar_title.text = timeline.title
        presenter = TimelinePresenter(this, twitter, timeline)
        updateTimelineMenu()
        initializeTimeline()
    }

    fun showTimelineSelector() {
        TimelineSelectDialog.newInstance()
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TimelineSelectDialog")
    }

    override fun showListsSelector(lists: List<TwList>) {
        ListsSelectDialog.newInstance(lists)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "ListsSelectDialog")
    }

    override fun onTimelineSelect(timeline: TimelineParameter) {
        prefs.addTimeline(timeline)
        switchTimeline(timeline)
    }

    override fun onListsSelectorOpen() = presenter.loadLists(prefs.currentUserId)

    override fun showListsLoading() {
        listsLoading = AlertDialog.Builder(this)
            .setView(R.layout.lists_loading_dialog)
            .create()
        listsLoading?.show()
    }

    override fun dismissListsLoading() {
        listsLoading?.dismiss()
        listsLoading = null
    }

    override fun onSearchInputOpen() {
        TextInputDialog.newInstance(TimelineParameter.TYPE_SEARCH, R.string.input_dialog_search)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TextInputDialog")
    }

    override fun onScreenNameInputOpen() {
        TextInputDialog.newInstance(TimelineParameter.TYPE_USER, R.string.input_dialog_user)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TextInputDialog")
    }

    fun showTimelineSettingDialog() {
        TimelineSettingDialog.newInstance(prefs.currentTimeline)
            .setTimelineSettingListener(this)
            .show(supportFragmentManager, "TimelineSettingDialog")
    }

    override fun onSaveTimeline(timeline: TimelineParameter) = switchTimeline(timeline)

    override fun onBackPressed() = toggleDrawer()

    private fun toggleDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    /**
     * Load initial tweets from api
     */
    override fun onRefresh() = presenter.loadTweets()

    /**
     * Load paginated tweets from api
     */
    override fun onLoadMore() = presenter.loadTweets(lastTweetId)

    override fun setLastTweetId(id: Long?) {
        this.lastTweetId = id
    }

    /**
     * Set initial tweets
     */
    override fun setTweets(tweets: List<Tweet>) {
        timelineAdapter.set(tweets)
        swipe_refresh.isRefreshing = false
    }

    /**
     * Add paginated tweets
     */
    override fun addTweets(tweets: List<Tweet>) = timelineAdapter.add(tweets)

    override fun stopLoading() = endlessScrollListener.stopLoading()

    fun initializeTimeline() {
        endlessScrollListener.reset()
        timelineAdapter.clear()
        timeline_list.scrollToPosition(0)
        swipe_refresh.post({
            swipe_refresh.isRefreshing = true
            onRefresh()
        })
    }

    override fun onUrlClick(url: String) {
        val backIcon = getVectorDrawable(R.drawable.ic_arrow_back, android.R.color.white).toBitmap()
        val chromeIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setCloseButtonIcon(backIcon)
            .addDefaultShareMenuItem()
            .build()

        chromeIntent.launchUrl(this, Uri.parse(url))
    }

    override fun onImageClick(images: List<Media>, pos: Int) = startActivity(GalleryActivity.newIntent(this, images, pos))

    override fun onVideoClick(video: VideoInfo) = startActivity(VideoPlayerActivity.newIntent(this, video))

    override fun onReplyClick(tweet: Tweet) = startActivity(StatusUpdateActivity.newIntent(this, tweet.id, tweet.user.screenName))

    override fun onLikeClick(tweet: Tweet) = presenter.like(tweet)

    override fun onRetweetClick(tweet: Tweet) = presenter.retweet(tweet)

    override fun onScreenNameClick(screenName: String) = onTimelineSelect(TimelineParameter.user(screenName))

    override fun onHashTagClick(hashTag: String) = onTimelineSelect(TimelineParameter.search(hashTag))

    override fun onShareClick(tweet: Tweet) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, tweet.permalinkUrl)
        startActivity(Intent.createChooser(i, resources.getString(R.string.share_tweet)))
    }

    override fun updateReactedTweet() = timelineAdapter.notifyDataSetChanged()

    override fun handleError(error: Throwable) {
        toast(error)
        swipe_refresh.isRefreshing = false
        endlessScrollListener.stopLoading()
    }
}

