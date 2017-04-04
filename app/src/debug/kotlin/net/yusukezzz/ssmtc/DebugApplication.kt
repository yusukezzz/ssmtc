package net.yusukezzz.ssmtc

import android.app.Activity
import android.os.Bundle
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import net.yusukezzz.ssmtc.ui.media.video.VideoPlayerActivity

class DebugApplication: Application() {
    override fun installLeakCanary(): RefWatcher {
        val watcher = LeakCanary.refWatcher(this).build()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {}

            override fun onActivityResumed(activity: Activity?) {}

            override fun onActivityStarted(activity: Activity?) {}

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

            override fun onActivityStopped(activity: Activity?) {}

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

            override fun onActivityDestroyed(activity: Activity?) {
                // ignore VideoView context leak
                if (activity is VideoPlayerActivity) {
                    return
                }
                watcher.watch(activity)
            }
        })

        return watcher
    }
}
