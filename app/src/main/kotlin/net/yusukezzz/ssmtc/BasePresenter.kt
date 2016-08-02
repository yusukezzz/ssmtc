package net.yusukezzz.ssmtc

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

interface BasePresenter {
    fun handleError(error: Throwable)

    infix fun <V, E> Promise<V, E>.doneUi(body: (value: V) -> Unit): Promise<V, E> {
        return successUi(body).failUi { handleError(it as Throwable) }
    }
}
