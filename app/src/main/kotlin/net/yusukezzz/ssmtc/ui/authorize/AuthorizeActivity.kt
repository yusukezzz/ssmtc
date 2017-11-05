package net.yusukezzz.ssmtc.ui.authorize

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.authorize.*
import kotlinx.android.synthetic.main.base_layout.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.data.repository.TimelineRepository
import net.yusukezzz.ssmtc.util.setView
import net.yusukezzz.ssmtc.util.toast
import javax.inject.Inject

class AuthorizeActivity : AppCompatActivity(), AuthorizeContract.View {
    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: TwitterService

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    @Inject
    lateinit var timelineRepo: TimelineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.inject(this)

        setContentView(R.layout.base_layout)
        main_contents.setView(R.layout.authorize)

        toolbar_title.text = "Authorization"
        val presenter = AuthorizePresenter(this, prefs, twitter, timelineRepo, accountRepo)

        btn_authorize_request.setOnClickListener { presenter.authorizeRequest() }
        btn_authorize.setOnClickListener { presenter.authorize(edit_pin_code.text.toString()) }
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
        toast(error)
    }
}
