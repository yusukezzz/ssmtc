package net.yusukezzz.ssmtc.screens.authorize

import net.yusukezzz.ssmtc.BasePresenter
import net.yusukezzz.ssmtc.BaseView

interface AuthorizeContract {
    interface Presenter: BasePresenter {
        fun authorizeRequest()
        fun authorize(pin: String)
    }

    interface View: BaseView<Presenter> {
        fun showAuthorizeWeb(url: String)
        fun authorized()
    }
}
