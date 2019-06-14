package net.yusukezzz.ssmtc

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*

class LifecycleScope(private val owner: LifecycleOwner): LifecycleObserver, CoroutineScope by MainScope() {
    init {
        owner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        owner.lifecycle.removeObserver(this)
    }

    fun bindLaunch(start: CoroutineStart = CoroutineStart.DEFAULT,
                   block: suspend CoroutineScope.() -> Unit): Job = launch(coroutineContext, start, block)
}

interface LifeCycleScopeSupport {
    val scope: LifecycleScope
}