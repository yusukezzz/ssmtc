package net.yusukezzz.ssmtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope

interface BasePresenter {
    val view: BaseView

    fun launch(start: CoroutineStart = CoroutineStart.DEFAULT,
               block: suspend CoroutineScope.() -> Unit) = view.scope.bindLaunch(start, block)
}

interface BaseView: LifeCycleScopeSupport
