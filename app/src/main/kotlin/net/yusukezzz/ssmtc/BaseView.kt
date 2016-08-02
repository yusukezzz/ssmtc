package net.yusukezzz.ssmtc

interface BaseView<T> {
    fun setPresenter(presenter: T)
    fun handleError(error: Throwable)
}
