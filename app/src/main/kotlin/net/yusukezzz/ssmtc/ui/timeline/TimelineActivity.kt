package net.yusukezzz.ssmtc.ui.timeline

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.base_layout.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.timeline_layout.*
import kotlinx.android.synthetic.main.timeline_list.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.LifecycleScope
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.data.api.model.TwList
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.VideoInfo
import net.yusukezzz.ssmtc.data.og.OpenGraphService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.media.photo.gallery.GalleryActivity
import net.yusukezzz.ssmtc.ui.media.video.VideoPlayerActivity
import net.yusukezzz.ssmtc.ui.status.update.FailureReceiver
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateService
import net.yusukezzz.ssmtc.ui.status.update.SuccessReceiver
import net.yusukezzz.ssmtc.ui.timeline.dialogs.*
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback
import java.io.File
import javax.inject.Inject

class TimelineActivity : DaggerAppCompatActivity(),
    TimelineContract.View,
    SwipeRefreshLayout.OnRefreshListener,
    PagingRecyclerOnScrollListener.ScrollListener,
    TweetItemView.TweetItemListener,
    NavigationView.OnNavigationItemSelectedListener,
    TimelineSettingDialog.TimelineSettingListener,
    BaseDialogFragment.TimelineSelectListener {

    companion object {
        const val STATE_OLDEST_TWEET_ID = "state_oldest_tweet_id"
        const val STATE_RECYCLER_VIEW = "state_recycler_view"
    }

    override val mainScope: LifecycleScope = LifecycleScope(this)

    private lateinit var pagingScrollListener: PagingRecyclerOnScrollListener
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
    lateinit var accountRepo: SsmtcAccountRepository

    @Inject
    lateinit var presenter: TimelineContract.Presenter

    @Inject
    lateinit var og: OpenGraphService

    private val successReceiver: BroadcastReceiver = SuccessReceiver()
    private val failureReceiver: BroadcastReceiver = FailureReceiver()

    private fun currentAccount(): SsmtcAccount = accountRepo.find(prefs.currentUserId)!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.plus(TimelineModule(this)).inject(this)

        if (prefs.currentUserId == 0L) {
            launchAuthorizeActivity()
            finish()
            return
        }

        setContentView(R.layout.timeline_layout)
        main_contents.setView(R.layout.timeline_list)
        setSupportActionBar(toolbar)

        setupDrawerView()
        setupTimelineView()

        if (savedInstanceState != null && lastTimelineFile.exists()) {
            loadAccount(init = false)
            restoreTimeline(savedInstanceState)
        } else {
            // initial load
            loadAccount()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // save last tweets
        val json = gson.toJson(timelineAdapter.getAll())
        lastTimelineFile.writeText(json)
        // save last scroll position
        outState.putParcelable(STATE_RECYCLER_VIEW, timeline_list.layoutManager!!.onSaveInstanceState())
        // save oldest tweet id
        lastTweetId?.let { outState.putLong(STATE_OLDEST_TWEET_ID, it) }
    }

    private fun restoreTimeline(state: Bundle) {
        val current = currentAccount().currentTimeline()
        toolbar_title.text = current.title
        presenter.setTimeline(current)
        updateTimelineMenu()

        // load tweets from file
        val json = lastTimelineFile.readText()
        val tweets: List<Tweet> = gson.fromJson(json, object : TypeToken<List<Tweet>>() {}.type)
        val timelineState = state.getParcelable<Parcelable>(STATE_RECYCLER_VIEW)
        timelineAdapter.set(tweets)
        timeline_list.layoutManager!!.onRestoreInstanceState(timelineState)
        val oldestTweetId = state.getLong(STATE_OLDEST_TWEET_ID)
        if (oldestTweetId != 0L) {
            lastTweetId = oldestTweetId
        }

        stopLoading()
    }

    private fun setupDrawerView() {
        val drawerToggle = object : ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close
        ) {
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
        timeline_list.adapter = TimelineAdapter(this, og).apply { setHasStableIds(true) }
        timeline_list.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        timeline_list.layoutManager = layoutManager
        val decoration = DividerItemDecoration(this, layoutManager.orientation)
        decoration.setDrawable(getCompatDrawable(R.drawable.timeline_divider))
        timeline_list.addItemDecoration(decoration)

        pagingScrollListener = PagingRecyclerOnScrollListener(layoutManager)
        pagingScrollListener.setLoadMoreListener(this)
        timeline_list.addOnScrollListener(pagingScrollListener)

        swipe_refresh.setOnRefreshListener(this)
        swipe_refresh.setColorSchemeResources(
            R.color.green,
            R.color.red,
            R.color.blue,
            R.color.yellow
        )

        toolbar_title.setOnClickListener {
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
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(successReceiver, IntentFilter(StatusUpdateService.ACTION_SUCCESS))
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(failureReceiver, IntentFilter(StatusUpdateService.ACTION_FAILURE))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(successReceiver)
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(failureReceiver)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_timeline, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove_timeline -> {
                val account = currentAccount()
                if (account.timelines.size == 1) {
                    toast("タイムラインは1つ以上必要です")
                } else {
                    accountRepo.update(account.withoutCurrentTimeline())
                    switchTimeline(currentAccount().currentTimeline())
                }
            }
            R.id.setting_timeline -> showTimelineSettingDialog()
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer.closeDrawer(GravityCompat.START)

        if (item.isChecked) {
            return true
        }

        return when (item.groupId) {
            R.id.menu_account -> handleAccountNavigation(item)
            R.id.menu_timeline -> handleTimelineNavigation(item)
            R.id.menu_manage -> handleManageNavigation(item)
            else -> false
        }
    }

    /**
     * Switch account to selected one from drawer navigation
     */
    private fun handleAccountNavigation(item: MenuItem): Boolean {
        val account = (accountRepo.findAll() - currentAccount())[item.order]
        prefs.currentUserId = account.user.id
        loadAccount()

        return false
    }

    /**
     * Switch timeline to selected one from drawer navigation
     */
    private fun handleTimelineNavigation(item: MenuItem): Boolean {
        val account = currentAccount()
        val timeline = account.timelines[item.order]
        accountRepo.update(account.copy(currentTimelineUuid = timeline.uuid))
        switchTimeline(timeline)

        return true
    }

    private fun handleManageNavigation(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_account_add -> launchAuthorizeActivity()
            R.id.nav_account_remove -> confirmRemoveAccount()
            R.id.nav_timeline_selector -> showTimelineSelector()
            else -> toast("unknown menu item: ${item.title}")
        }

        return false
    }

    private fun launchAuthorizeActivity() = startActivity(Intent(this, AuthorizeActivity::class.java))

    private fun loadAccount(init: Boolean = true) {
        val account = currentAccount()
        presenter.resetIgnoreIds()
        presenter.setTokens(account.credentials)

        val headerView = nav_view.getHeaderView(0)
        val profileImage: ImageView = headerView.findViewById(R.id.profile_image)
        val screenName: TextView = headerView.findViewById(R.id.screen_name)
        val accountSelectBtn: ImageView = headerView.findViewById(R.id.btn_account_selector)
        PicassoUtil.userIcon(account.user, profileImage)
        screenName.text = account.user.screenName

        accountSelectBtn.setOnClickListener {
            toggleNavigationContents()
        }

        if (init) {
            switchTimeline(account.currentTimeline())
        }
    }

    /**
     * toggle timeline and account navigation menu
     */
    private fun toggleNavigationContents() {
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

    private fun showAccountNavigation() {
        nav_view.menu.clear()
        btn_account_selector.setImageResource(R.drawable.ic_arrow_drop_up)
        nav_view.inflateMenu(R.menu.menu_drawer_account)
        (accountRepo.findAll() - currentAccount()).forEachIndexed { i, account ->
            nav_view.menu.add(R.id.menu_account, Menu.NONE, i, account.user.screenName)
        }
    }

    private fun confirmRemoveAccount() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_account_remove_message)
            .setPositiveButton(R.string.confirm_account_remove_ok) { _, _ -> removeAccount() }
            .setNegativeButton(R.string.confirm_account_remove_cancel) { _, _ -> /* do nothing */ }
            .show()
    }

    private fun removeAccount() {
        accountRepo.delete(currentAccount())
        if (accountRepo.findAll().isEmpty()) {
            launchAuthorizeActivity()
        } else {
            prefs.currentUserId = accountRepo.findAll().first().user.id
            loadAccount()
        }
    }

    private fun updateTimelineMenu() {
        nav_view.menu.removeGroup(R.id.menu_timeline)
        val account = currentAccount()
        account.timelines.forEachIndexed { index, timeline ->
            nav_view.menu.add(R.id.menu_timeline, Menu.NONE, index, timeline.title)
                .setCheckable(true)
                .setIcon(timelineIcon(timeline.type))
                .isChecked = (timeline.uuid == account.currentTimelineUuid)
        }
    }

    private fun timelineIcon(type: Int): Int = when (type) {
        Timeline.TYPE_HOME -> R.drawable.ic_timeline_home
        Timeline.TYPE_MENTIONS -> R.drawable.ic_timeline_mention
        Timeline.TYPE_LISTS -> R.drawable.ic_timeline_list
        Timeline.TYPE_SEARCH -> R.drawable.ic_timeline_search
        Timeline.TYPE_USER -> R.drawable.ic_timeline_user
        else -> R.drawable.ic_timeline_home
    }

    private fun switchTimeline(timeline: Timeline) {
        toolbar_title.text = timeline.title
        presenter.setTimeline(timeline)
        updateTimelineMenu()
        initializeTimeline()
    }

    private fun showTimelineSelector() {
        TimelineSelectDialog.newInstance()
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TimelineSelectDialog")
    }

    override fun showListsSelector(lists: List<TwList>) {
        ListsSelectDialog.newInstance(lists)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "ListsSelectDialog")
    }

    override fun onTimelineSelect(timeline: Timeline) {
        val account = currentAccount()
        accountRepo.update(account.copy(timelines = account.timelines + timeline, currentTimelineUuid = timeline.uuid))
        switchTimeline(timeline)
    }

    override fun onListsSelectorOpen() {
        presenter.loadLists(prefs.currentUserId)
    }

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
        TextInputDialog.newInstance(Timeline.TYPE_SEARCH, R.string.input_dialog_search)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TextInputDialog")
    }

    override fun onScreenNameInputOpen() {
        TextInputDialog.newInstance(Timeline.TYPE_USER, R.string.input_dialog_user)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TextInputDialog")
    }

    private fun showTimelineSettingDialog() {
        TimelineSettingDialog.newInstance(currentAccount().currentTimeline())
            .setTimelineSettingListener(this)
            .show(supportFragmentManager, "TimelineSettingDialog")
    }

    override fun onSaveTimeline(timeline: Timeline) {
        val account = currentAccount()
        accountRepo.update(account.copy(
            timelines = account.timelines.filterNot { it.uuid == timeline.uuid } + timeline,
            currentTimelineUuid = timeline.uuid))
        switchTimeline(timeline)
    }

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
    override fun onRefresh() {
        presenter.loadTweets()
    }

    /**
     * Load paginated tweets from api
     */
    override fun onLoadMore() {
        presenter.loadTweets(lastTweetId)
    }

    override fun setLastTweetId(id: Long?) {
        this.lastTweetId = id
    }

    /**
     * Set initial tweets
     */
    override fun setTweets(tweets: List<Tweet>) {
        pagingScrollListener.reset()
        timelineAdapter.set(tweets)
        swipe_refresh.isRefreshing = false
    }

    /**
     * Add paginated tweets
     */
    override fun addTweets(tweets: List<Tweet>) = timelineAdapter.add(tweets)

    override fun timelineEdgeReached() {
        pagingScrollListener.disable()
        toast(resources.getString(R.string.end_of_timeline_reached))
    }

    override fun rateLimitExceeded() {
        pagingScrollListener.disable()
        toast(resources.getString(R.string.rate_limit_exceeded))
    }

    override fun stopLoading() = pagingScrollListener.stopLoading()

    private fun initializeTimeline() {
        pagingScrollListener.reset()
        timelineAdapter.clear()
        timeline_list.scrollToPosition(0)
        swipe_refresh.post {
            swipe_refresh.isRefreshing = true
            onRefresh()
        }
    }

    override fun onUrlClick(url: String) {
        val backIcon = getVectorDrawable(R.drawable.ic_arrow_back, android.R.color.white).toBitmap()
        val chromeIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setCloseButtonIcon(backIcon)
            .addDefaultShareMenuItem()
            .build()

        CustomTabsHelper.addKeepAliveExtra(this, chromeIntent.intent)
        CustomTabsHelper.openCustomTab(this, chromeIntent, Uri.parse(url), WebViewFallback())
    }

    override fun onImageClick(images: List<Media>, pos: Int) =
        startActivity(GalleryActivity.newIntent(this, images, pos))

    override fun onVideoClick(video: VideoInfo) = startActivity(VideoPlayerActivity.newIntent(this, video))

    override fun onReplyClick(tweet: Tweet) =
        startActivity(StatusUpdateActivity.newIntent(this, tweet.id, tweet.user.screenName))

    override fun onLikeClick(tweet: Tweet) = presenter.like(tweet)

    override fun onRetweetClick(tweet: Tweet) = presenter.retweet(tweet)

    override fun onScreenNameClick(screenName: String) = onTimelineSelect(Timeline.user(screenName))

    override fun onHashTagClick(hashTag: String) = onTimelineSelect(Timeline.search(hashTag))

    override fun onShareClick(tweet: Tweet) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, tweet.permalinkUrl)
        startActivity(Intent.createChooser(i, resources.getString(R.string.share_tweet)))
    }

    override fun updateReactedTweet() = timelineAdapter.notifyDataSetChanged()

    override fun handleError(error: Throwable) {
        snackbar(error)
        swipe_refresh.isRefreshing = false
        pagingScrollListener.stopLoading()
    }
}

