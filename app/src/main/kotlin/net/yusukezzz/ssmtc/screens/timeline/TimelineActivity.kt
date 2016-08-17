package net.yusukezzz.ssmtc.screens.timeline

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.main_content.*
import kotlinx.android.synthetic.main.main_drawer.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.Account
import net.yusukezzz.ssmtc.screens.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.screens.timeline.dialogs.*
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.util.PreferencesHolder
import net.yusukezzz.ssmtc.util.picasso.CircleTransformation
import net.yusukezzz.ssmtc.util.toast
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class TimelineActivity: AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    TimelineSettingDialog.TimelineSettingListener,
    AccountSelectDialog.AccountSelectListener,
    TimelineFragment.TimelineFragmentListener,
    BaseDialogFragment.TimelineSelectListener {

    private lateinit var presenter: TimelineContract.Presenter
    private val app: Application by lazy { application as Application }
    private val prefs: Preferences by lazy { PreferencesHolder.prefs }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (null == prefs.currentAccount) {
            launchAuthorizeActivity()
            return
        }

        setContentView(R.layout.main_drawer)
        setSupportActionBar(toolbar)

        val drawerToggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close)
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        var fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (null == fragment) {
            fragment = TimelineFragment.newInstance(this)
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }

        presenter = TimelinePresenter(fragment as TimelineFragment, app.twitter)
    }

    override fun onTimelineReady() {
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
        drawer_layout.closeDrawer(GravityCompat.START)

        if (item.isChecked) {
            return true
        }

        when (item.groupId) {
            R.id.menu_timeline -> return handleTimelineNavigation(item)
            R.id.menu_manage -> return handleManageNavigation(item)
            else -> return false
        }
    }

    fun handleTimelineNavigation(item: MenuItem): Boolean {
        prefs.currentTimelineIndex = item.order
        switchTimeline(prefs.currentTimeline)
        return true
    }

    fun handleManageNavigation(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_timeline_selector -> showTimelineSelector()
            R.id.nav_account_selector -> showAccountSelector()
            R.id.nav_account_remove -> confirmRemoveAccount()
        }

        return false
    }

    fun launchAuthorizeActivity() {
        val i = Intent(this, AuthorizeActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
        finish()
    }

    fun loadAccount() {
        val account = prefs.currentAccount!!
        app.twitter.setTokens(account.accessToken, account.secretToken)

        val profileImage = nav_view.getHeaderView(0).findViewById(R.id.profile_image) as ImageView
        val screenName = nav_view.getHeaderView(0).findViewById(R.id.screen_name) as TextView
        Picasso.with(this)
            .load(account.user.profileImageUrl)
            .transform(CircleTransformation())
            .fit()
            .centerCrop()
            .into(profileImage)
        screenName.text = account.user.screenName

        switchTimeline(prefs.currentTimeline)
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
        presenter.setParameter(timeline)
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

        task {
            val userId = prefs.currentUserId
            val ownedTwList = app.twitter.ownedLists(userId)
            val subscribedTwList = app.twitter.subscribedLists(userId)
            ownedTwList + subscribedTwList
        } successUi {
            ListsSelectDialog.newInstance(it)
                .setTimelineSelectListener(this)
                .show(supportFragmentManager, "ListsSelectDialog")
        } failUi {
            println(it)
            toast(it.message)
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

    fun showAccountSelector() {
        AccountSelectDialog.newInstance(prefs.accounts)
            .setAccountSelectListener(this)
            .show(supportFragmentManager, "AccountSelectDialog")
    }

    override fun onAccountSelect(account: Account) {
        if (prefs.currentUserId != account.user.id) {
            prefs.currentUserId = account.user.id
            loadAccount()
        }
    }

    override fun onAccountAdd() {
        launchAuthorizeActivity()
    }

    fun showTimelineSettingDialog() {
        TimelineSettingDialog.newInstance(prefs.currentTimeline)
            .setTimelineSettingListener(this)
            .show(supportFragmentManager, "TimelineSettingDialog")
    }

    override fun onSaveTimeline(timeline: TimelineParameter) {
        switchTimeline(timeline)
    }
}

