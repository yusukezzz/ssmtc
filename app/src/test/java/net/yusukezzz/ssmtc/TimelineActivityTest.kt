package net.yusukezzz.ssmtc

import android.content.ComponentName
import net.yusukezzz.ssmtc.di.TestAppModule
import net.yusukezzz.ssmtc.ui.authorize.AuthorizeActivity
import net.yusukezzz.ssmtc.ui.timeline.TimelineActivity
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
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
}
