package net.yusukezzz.ssmtc.ui.timeline

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.base_layout.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.timeline_layout.*
import kotlinx.android.synthetic.main.timeline_list.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.Tweet
import net.yusukezzz.ssmtc.data.json.VideoInfo
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.media.photo.gallery.GalleryActivity
import net.yusukezzz.ssmtc.ui.media.video.VideoPlayerActivity
import net.yusukezzz.ssmtc.ui.status.update.StatusUpdateActivity
import net.yusukezzz.ssmtc.ui.timeline.dialogs.*
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.RoundedTransformation
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class TimelineActivity: AppCompatActivity(),
    TimelineContract.View,
    SwipeRefreshLayout.OnRefreshListener,
    EndlessRecyclerOnScrollListener.ScrollListener,
    TimelineAdapter.TimelineEventListener,
    NavigationView.OnNavigationItemSelectedListener,
    TimelineSettingDialog.TimelineSettingListener,
    BaseDialogFragment.TimelineSelectListener {

    private lateinit var presenter: TimelineContract.Presenter
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private val timelineAdapter: TimelineAdapter by lazy { timeline_list.adapter as TimelineAdapter }
    private val app: Application by lazy { application as Application }
    private val prefs: Preferences by lazy { PreferencesHolder.prefs }

    // Oldest tweet id on current timeline
    private var lastTweetId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (null == prefs.currentAccount) {
            launchAuthorizeActivity()
            finish()
            return
        }

        setContentView(R.layout.timeline_layout)
        main_contents.setView(R.layout.timeline_list)
        setSupportActionBar(toolbar)

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

        val layoutManager = LinearLayoutManager(this)
        endlessScrollListener = EndlessRecyclerOnScrollListener(this, layoutManager)
        endlessScrollListener.setLoadMoreListener(this)
        timeline_list.setHasFixedSize(true)
        timeline_list.layoutManager = layoutManager
        timeline_list.addOnScrollListener(endlessScrollListener)
        timeline_list.adapter = TimelineAdapter(this)

        swipe_refresh.setOnRefreshListener(this)
        swipe_refresh.setColorSchemeResources(R.color.green, R.color.red, R.color.blue, R.color.yellow)

        findViewById(R.id.toolbar_title).setOnClickListener {
            timeline_list.scrollToPosition(0)
        }

        tweet_btn.setOnClickListener {
            val i = Intent(this, StatusUpdateActivity::class.java)
            startActivity(i)
        }

        loadAccount()
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
        val accounts = (prefs.accounts - prefs.currentAccount!!)
        prefs.currentUserId = accounts[item.order].user.id
        loadAccount()
        showTimelineNavigation()

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

    fun launchAuthorizeActivity() {
        startActivity(Intent(this, AuthorizeActivity::class.java))
    }

    fun loadAccount() {
        val account = prefs.currentAccount!!
        app.twitter.setTokens(account.accessToken, account.secretToken)

        val profileImage = nav_view.getHeaderView(0).findViewById(R.id.profile_image) as ImageView
        val screenName = nav_view.getHeaderView(0).findViewById(R.id.screen_name) as TextView
        val accountSelectBtn = nav_view.getHeaderView(0).findViewById(R.id.btn_account_selector) as ImageView
        Picasso.with(this)
            .load(account.user.profileImageUrl)
            .fit()
            .centerCrop()
            .transform(RoundedTransformation(8))
            .into(profileImage)
        screenName.text = account.user.screenName

        accountSelectBtn.setOnClickListener {
            toggleNavigationContents()
        }

        switchTimeline(prefs.currentTimeline)
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
        (prefs.accounts - prefs.currentAccount!!).forEachIndexed { i, account ->
            nav_view.menu.add(R.id.menu_account, Menu.NONE, i, account.user.screenName)
        }
    }

    fun confirmRemoveAccount() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_account_remove_message)
            .setPositiveButton(R.string.confirm_account_remove_ok, { dialog, which -> removeAccount() })
            .setNegativeButton(R.string.confirm_account_remove_cancel, { d, w -> /* do nothing */ })
            .show()
    }

    fun removeAccount() {
        prefs.removeCurrentAccount()
        if (null == prefs.currentAccount) {
            launchAuthorizeActivity()
        } else {
            loadAccount()
        }
    }

    fun updateTimelineMenu() {
        nav_view.menu.removeGroup(R.id.menu_timeline)
        prefs.currentAccount!!.timelines.forEachIndexed { index, timeline ->
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
        presenter = TimelinePresenter(this, app.twitter, timeline)
        updateTimelineMenu()
    }

    fun showTimelineSelector() {
        TimelineSelectDialog.newInstance()
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TimelineSelectDialog")
    }

    override fun onTimelineSelected(timeline: TimelineParameter) {
        prefs.addTimeline(timeline)
        switchTimeline(timeline)
    }

    override fun openListsDialog() {
        val progress = AlertDialog.Builder(this)
            .setView(R.layout.lists_loading_dialog)
            .create()
        progress.show()

        val userId = prefs.currentUserId
        task {
            app.twitter.ownedLists(userId)
        } and task {
            app.twitter.subscribedLists(userId)
        } successUi {
            ListsSelectDialog.newInstance(it.first + it.second)
                .setTimelineSelectListener(this)
                .show(supportFragmentManager, "ListsSelectDialog")
        } failUi {
            toast(it)
        } alwaysUi {
            progress.dismiss()
        }
    }

    override fun openSearchInputDialog() {
        TextInputDialog.newInstance(TimelineParameter.TYPE_SEARCH, R.string.input_dialog_search)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TextInputDialog")
    }

    override fun openUserInputDialog() {
        TextInputDialog.newInstance(TimelineParameter.TYPE_USER, R.string.input_dialog_user)
            .setTimelineSelectListener(this)
            .show(supportFragmentManager, "TextInputDialog")
    }

    fun showTimelineSettingDialog() {
        TimelineSettingDialog.newInstance(prefs.currentTimeline)
            .setTimelineSettingListener(this)
            .show(supportFragmentManager, "TimelineSettingDialog")
    }

    override fun onSaveTimeline(timeline: TimelineParameter) {
        switchTimeline(timeline)
    }

    override fun onBackPressed() {
        toggleDrawer()
    }

    private fun toggleDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    override fun onRefresh() {
        presenter.loadTweets()
    }

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
     * Add more loaded tweets
     */
    override fun addTweets(tweets: List<Tweet>) {
        timelineAdapter.add(tweets)
        println("tweets pushed")
        // TODO: more loading progress off
    }

    override fun stopLoading() {
        endlessScrollListener.stopLoading()
    }

    override fun onLoadMore() {
        println("onLoadMore")
        presenter.loadTweets(lastTweetId)
        // TODO: more loading progress on
    }

    override fun initialize() {
        endlessScrollListener.reset()
        timelineAdapter.clear()
        timeline_list.scrollToPosition(0)
        swipe_refresh.post({
            swipe_refresh.isRefreshing = true
            onRefresh()
        })
    }

    override fun onUrlClick(url: String) {
        val urlIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, url)
        val pending = PendingIntent.getActivity(this, 0, urlIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val backIcon = getVectorDrawable(R.drawable.ic_arrow_back, android.R.color.white).toBitmap()
        val shareIcon = getVectorDrawable(R.drawable.ic_share, android.R.color.white).toBitmap()

        val chromeIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setCloseButtonIcon(backIcon)
            .setActionButton(shareIcon, "Share", pending)
            .build()

        chromeIntent.launchUrl(this, Uri.parse(url))
    }

    override fun onImageClick(images: List<String>, pos: Int) {
        startActivity(GalleryActivity.newIntent(this, images, pos))
    }

    override fun onVideoClick(video: VideoInfo) {
        startActivity(VideoPlayerActivity.newIntent(this, video))
    }

    override fun onReplyClick(tweet: Tweet) {
        startActivity(StatusUpdateActivity.newIntent(this, tweet.id, tweet.user.screenName))
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
        onTimelineSelected(TimelineParameter.user(screenName))
    }

    override fun onHashTagClick(hashTag: String) {
        onTimelineSelected(TimelineParameter.search(hashTag))
    }

    override fun updateReactedTweet() {
        timelineAdapter.notifyDataSetChanged()
    }

    override fun handleError(error: Throwable) {
        toast(error)
        swipe_refresh.isRefreshing = false
        endlessScrollListener.stopLoading()
    }
}

