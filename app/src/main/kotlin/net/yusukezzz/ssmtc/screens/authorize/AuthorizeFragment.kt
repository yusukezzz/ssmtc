package net.yusukezzz.ssmtc.screens.authorize

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.authorize.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.util.toast

class AuthorizeFragment: Fragment(), AuthorizeContract.View {
    companion object {
        fun newInstance() = AuthorizeFragment()
    }

    private lateinit var presenter: AuthorizeContract.Presenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.authorize, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_authorize_request.setOnClickListener { presenter.authorizeRequest() }
        btn_authorize.setOnClickListener { presenter.authorize(edit_pin_code.text.toString()) }
    }

    override fun setPresenter(presenter: AuthorizeContract.Presenter) {
        this.presenter = presenter
    }

    override fun showAuthorizeWeb(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    override fun authorized() {
        (activity as AuthorizeActivity).onAuthorized()
    }

    override fun handleError(error: Throwable) {
        println(error)
        error.message?.let { toast(it) }
    }
}
