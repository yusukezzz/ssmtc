package net.yusukezzz.ssmtc.screens.authorize

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.main_content.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.util.PreferencesHolder

class AuthorizeActivity: AppCompatActivity() {
    private val app: Application by lazy { application as Application }
    private lateinit var presenter: AuthorizeContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_content)

        var fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (null == fragment) {
            fragment = AuthorizeFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }

        toolbar_title.text = "Authorization"
        presenter = AuthorizePresenter(fragment as AuthorizeFragment, PreferencesHolder.prefs, app.twitter)
    }

    fun onAuthorized() {
        val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }
}
