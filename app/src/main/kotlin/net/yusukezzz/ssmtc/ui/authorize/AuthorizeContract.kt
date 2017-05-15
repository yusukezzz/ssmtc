package net.yusukezzz.ssmtc.ui.authorize

import net.yusukezzz.ssmtc.BasePresenter
import net.yusukezzz.ssmtc.data.Credential
import net.yusukezzz.ssmtc.data.api.model.User

interface AuthorizeContract {
    interface Presenter: BasePresenter {
        fun authorizeRequest()
        fun authorize(pin: String)
    }

    interface View {
        fun showAuthorizeWeb(url: String)
        fun authorized(credential: Credential, user: User)
        fun handleError(error: Throwable)
    }
}
