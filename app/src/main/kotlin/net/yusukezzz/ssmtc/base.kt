package net.yusukezzz.ssmtc

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import net.yusukezzz.ssmtc.util.launchUI

class LifecycleScope(private val owner: LifecycleOwner) : LifecycleObserver, CoroutineScope by MainScope() {
    init {
        owner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        coroutineContext.cancelChildren()
        owner.lifecycle.removeObserver(this)
    }
}

interface BasePresenter {
    val view: BaseView

    fun task(block: suspend CoroutineScope.() -> Unit, always: () -> Unit = {}): Job {
        return view.mainScope.launchUI {
            try {
                block()
            } catch (e: Throwable) {
                handleError(e)
            } finally {
                always()
            }
        }
    }

    fun handleError(error: Throwable) {
        view.handleError(error)
    }
}

interface BaseView {
    val mainScope: LifecycleScope

    fun handleError(error: Throwable)
}
