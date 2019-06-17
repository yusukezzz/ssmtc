package net.yusukezzz.ssmtc

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*

class LifecycleScope(private val owner: LifecycleOwner) : LifecycleObserver, CoroutineScope by MainScope() {
    init {
        owner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        cancel()
        owner.lifecycle.removeObserver(this)
    }
}

interface BasePresenter {
    val view: BaseView

    fun handleError(error: Throwable)

    fun task(block: suspend CoroutineScope.() -> Unit, always: () -> Unit = {}): Job {
        return view.launch {
            try {
                block()
            } catch (e: Throwable) {
                handleError(e)
            } finally {
                always()
            }
        }
    }

    suspend fun <T> CoroutineScope.async(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> =
        async(Dispatchers.IO, start, block)

    suspend fun <T> CoroutineScope.withIO(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.IO, block)
}

interface BaseView {
    val mainScope: LifecycleScope

    fun launch(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job =
        mainScope.launch(mainScope.coroutineContext, start, block)
}
