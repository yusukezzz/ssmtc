package net.yusukezzz.ssmtc.screens.authorize

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.authorize.*
import kotlinx.android.synthetic.main.base_layout.*
import kotlinx.android.synthetic.main.main_container.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.util.PreferencesHolder
import net.yusukezzz.ssmtc.util.toast

class AuthorizeActivity : AppCompatActivity(), AuthorizeContract.View {

    private val app: Application by lazy { application as Application }
    private lateinit var presenter: AuthorizeContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_layout)

        val authorizeView = layoutInflater.inflate(R.layout.authorize, main_container, false)
        main_container.addView(authorizeView, 0)

        toolbar_title.text = "Authorization"
        presenter = AuthorizePresenter(this, PreferencesHolder.prefs, app.twitter)

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
        val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }

    override fun handleError(error: Throwable) {
        println(error)
        error.message?.let { toast(it) }
    }
}
