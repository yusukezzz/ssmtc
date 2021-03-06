package net.yusukezzz.ssmtc

import android.content.ComponentName
import net.yusukezzz.ssmtc.data.Credentials
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.model.User
import net.yusukezzz.ssmtc.di.TestAppModule
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.timeline.TimelineActivity
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class TimelineActivityTest {
    private fun getModule(): TestAppModule = (RuntimeEnvironment.application as TestApplication).module
    private fun mockUser(id: Long = 1): User =
        User(id, "name", "screenName", false, false, "profileImage", "profileImageHttps", 0, 0, 0, 0, 0)

    private fun mockAccount(timelines: List<Timeline>): SsmtcAccount =
        SsmtcAccount(Credentials("dummyToken", "dummyTokenSecret"), mockUser(), timelines, timelines.first().uuid)

    @Test
    fun shouldStartAuthorizeIfNotLoggedIn() {
        val prefs = getModule().mockPrefs
        Mockito.`when`(prefs.currentUserId).thenReturn(0L)

        val act = Robolectric.buildActivity(TimelineActivity::class.java).create().get()
        val nextIntent = Shadows.shadowOf(act).peekNextStartedActivityForResult().intent

        assertThat(nextIntent.component, equalTo(ComponentName(act, AuthorizeActivity::class.java)))
    }

    @Test
    fun shouldLoadInitialTweetsIfLoggedIn() {
        val timelines = listOf(Timeline.home())
        val prefs = getModule().mockPrefs
        val accountRepo = getModule().mockAccountRepo
        Mockito.`when`(prefs.currentUserId).thenReturn(mockUser().id)
        Mockito.`when`(accountRepo.find(mockUser().id)).thenReturn(mockAccount(timelines))

        Robolectric.buildActivity(TimelineActivity::class.java).create().get()

        verify(getModule().mockTwitter).statuses(timelines.first(), null)
    }

    @Test
    fun shouldPagingRequestIfLastTweetIdExists() {
        val timelines = listOf(Timeline.home())
        val prefs = getModule().mockPrefs
        val accountRepo = getModule().mockAccountRepo
        Mockito.`when`(prefs.currentUserId).thenReturn(mockUser().id)
        Mockito.`when`(accountRepo.find(mockUser().id)).thenReturn(mockAccount(timelines))

        val lastTweetId = 100L
        val act = Robolectric.buildActivity(TimelineActivity::class.java).create().get()
        act.setLastTweetId(lastTweetId)
        act.onLoadMore()

        // wait async task
        Thread.sleep(100)

        verify(getModule().mockTwitter).statuses(timelines.first(), lastTweetId)
    }
}
