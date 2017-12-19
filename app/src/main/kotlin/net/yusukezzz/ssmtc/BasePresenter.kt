package net.yusukezzz.ssmtc

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import net.yusukezzz.ssmtc.util.ui
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

interface BasePresenter {
    fun handleError(error: Throwable)

    infix fun <V, E> Promise<V, E>.doneUi(body: (value: V) -> Unit): Promise<V, E> {
        return successUi(body).failUi { handleError(it as Throwable) }
    }

    fun execOnUi(body: suspend CoroutineScope.() -> Unit, always: () -> Unit = {}): Job {
        return ui {
            try {
                body()
            } catch (e: Throwable) {
                handleError(e)
            } finally {
                always()
            }
        }
    }
}
