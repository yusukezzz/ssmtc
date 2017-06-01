package net.yusukezzz.ssmtc.ui.authorize

import net.yusukezzz.ssmtc.BasePresenter

interface AuthorizeContract {
    interface Presenter: BasePresenter {
        fun authorizeRequest()
        fun authorize(pin: String)
    }

    interface View {
        fun showAuthorizeWeb(url: String)
        fun authorized()
        fun handleError(error: Throwable)
    }
}
