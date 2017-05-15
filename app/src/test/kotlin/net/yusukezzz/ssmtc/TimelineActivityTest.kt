package net.yusukezzz.ssmtc

import android.content.ComponentName
import net.yusukezzz.ssmtc.data.SsmtcAccount
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.model.Entity
import net.yusukezzz.ssmtc.data.api.model.Tweet
import net.yusukezzz.ssmtc.data.api.model.User
import net.yusukezzz.ssmtc.di.TestAppModule
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.timeline.TimelineActivity
import nl.komponents.kovenant.Kovenant
import nl.komponents.kovenant.testMode
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.threeten.bp.OffsetDateTime

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class)
class TimelineActivityTest {
    private val nowDateTime: OffsetDateTime = OffsetDateTime.now()

    private fun getModule(): TestAppModule = (RuntimeEnvironment.application as TestApplication).module
    private fun mockUser(id: Long = 1): User =
        User(id, "name", "screenName", false, false, "profileImage", "profileImageHttps", 0, 0, 0, 0, 0)

    private fun mockAccount(timelines: List<TimelineParameter>): SsmtcAccount =
        SsmtcAccount("dummyToken", "dummySecret", mockUser(), timelines, 0)

    private fun mockTweet(id: Long) =
        Tweet(id, "tweet $id", mockUser(), Entity(), Entity(), nowDateTime, null, null, 0, 0, false, false)

    @Before
    fun setup() {
        Kovenant.testMode() // all dispatchers are synchronous mode
    }

    @Test
    fun shouldStartAuthorizeIfNotLoggedIn() {
        val prefs = getModule().mockPrefs
        Mockito.`when`(prefs.getCurrentAccount()).thenReturn(null)

        val act = Robolectric.buildActivity(TimelineActivity::class.java).create().get()
        val nextIntent = Shadows.shadowOf(act).peekNextStartedActivityForResult().intent

        assertThat(nextIntent.component, equalTo(ComponentName(act, AuthorizeActivity::class.java)))
    }

    @Test
    fun shouldLoadInitialTweetsIfLoggedIn() {
        val timelines = listOf(TimelineParameter.home())
        val prefs = getModule().mockPrefs
        Mockito.`when`(prefs.getCurrentAccount()).thenReturn(mockAccount(timelines))
        Mockito.`when`(prefs.getCurrentTimeline()).thenReturn(timelines.first())

        Robolectric.buildActivity(TimelineActivity::class.java).create().get()

        verify(getModule().mockTwitter).timeline(timelines.first(), null)
    }

    @Test
    fun shouldPagingRequestIfLastTweetIdExists() {
        val timelines = listOf(TimelineParameter.home())
        val prefs = getModule().mockPrefs
        Mockito.`when`(prefs.getCurrentAccount()).thenReturn(mockAccount(timelines))
        Mockito.`when`(prefs.getCurrentTimeline()).thenReturn(timelines.first())

        val lastTweetId = 100L
        val act = Robolectric.buildActivity(TimelineActivity::class.java).create().get()
        act.setLastTweetId(lastTweetId)
        act.onLoadMore()

        verify(getModule().mockTwitter).timeline(timelines.first(), lastTweetId)
    }
}
