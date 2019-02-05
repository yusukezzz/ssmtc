package net.yusukezzz.ssmtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.yusukezzz.ssmtc.util.ui

interface BasePresenter {
    fun handleError(error: Throwable)

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
