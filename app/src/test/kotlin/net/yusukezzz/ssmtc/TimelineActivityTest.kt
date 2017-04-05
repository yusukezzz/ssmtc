package net.yusukezzz.ssmtc

import android.content.ComponentName
import net.yusukezzz.ssmtc.data.Account
import net.yusukezzz.ssmtc.data.api.TimelineParameter
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class)
class TimelineActivityTest {
    private fun getModule(): TestAppModule = (RuntimeEnvironment.application as TestApplication).module

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
        val user = User(1, "name", "screenName", false, false, "profileImage", "profileImageHttps", 0, 0, 0, 0, 0)
        val timelines = listOf(TimelineParameter.home())
        val account = Account("dummyToken", "dummySecret", user, timelines, 0)
        val prefs = getModule().mockPrefs
        Mockito.`when`(prefs.getCurrentAccount()).thenReturn(account)
        Mockito.`when`(prefs.getCurrentTimeline()).thenReturn(timelines.first())

        Robolectric.buildActivity(TimelineActivity::class.java).create().get()

        verify(getModule().mockTwitter).timeline(timelines.first(), null)
    }
}
