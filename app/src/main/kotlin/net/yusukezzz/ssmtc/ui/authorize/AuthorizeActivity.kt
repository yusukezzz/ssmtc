package net.yusukezzz.ssmtc.ui.authorize

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.LifecycleScope
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.data.api.TwitterService
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.data.repository.TimelineRepository
import net.yusukezzz.ssmtc.databinding.AuthorizeBinding
import net.yusukezzz.ssmtc.databinding.BaseLayoutBinding
import net.yusukezzz.ssmtc.util.snackbar
import javax.inject.Inject

class AuthorizeActivity : AppCompatActivity(), AuthorizeContract.View {
    override val mainScope: LifecycleScope = LifecycleScope(this)

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var twitter: TwitterService

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    @Inject
    lateinit var timelineRepo: TimelineRepository

    private lateinit var baseLayout: BaseLayoutBinding
    private lateinit var authorizeLayout: AuthorizeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.inject(this)

        baseLayout = BaseLayoutBinding.inflate(layoutInflater)
        authorizeLayout = AuthorizeBinding.inflate(layoutInflater)
        setContentView(baseLayout.root)
        baseLayout.mainContents.addView(authorizeLayout.root)

        baseLayout.toolbarTitle.text = "Authorization"
        val presenter = AuthorizePresenter(this, prefs, twitter, timelineRepo, accountRepo)

        authorizeLayout.apply {
            btnAuthorizeRequest.setOnClickListener { presenter.authorizeRequest() }
            btnAuthorize.setOnClickListener { presenter.authorize(editPinCode.text.toString()) }
        }
    }

    override fun showAuthorizeWeb(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    override fun authorized() {
        val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)!!
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }

    override fun handleError(error: Throwable) {
        snackbar(error)
    }
}
