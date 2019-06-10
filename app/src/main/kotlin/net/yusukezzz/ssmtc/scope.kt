package net.yusukezzz.ssmtc

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException
import kotlin.coroutines.CoroutineContext

object AndroidScope: CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main
}

class LifecycleScope(private val owner: LifecycleOwner,
                     coroutineScope: CoroutineScope = AndroidScope): LifecycleObserver, CoroutineScope {
    init {
        coroutineScope.coroutineContext[Job]?.let {
            throw IllegalArgumentException("A Context with Job already passed")
        }
        owner.lifecycle.addObserver(this)
    }

    private val master: CompletableJob = SupervisorJob()

    override val coroutineContext: CoroutineContext = (coroutineScope + master).coroutineContext

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        master.cancel()
        owner.lifecycle.removeObserver(this)
    }

    fun bindLaunch(start: CoroutineStart = CoroutineStart.DEFAULT,
                   block: suspend CoroutineScope.() -> Unit): Job = launch(coroutineContext, start, block)
}