package net.yusukezzz.ssmtc.ui.status.update

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.status_update.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.ui.media.photo.selector.PhotoSelectorActivity
import net.yusukezzz.ssmtc.ui.misc.AspectRatioImageView
import net.yusukezzz.ssmtc.util.getExtraStreamOrNull
import net.yusukezzz.ssmtc.util.getImagePath
import net.yusukezzz.ssmtc.util.getLongExtraOrNull
import net.yusukezzz.ssmtc.util.getStringExtraOrNull
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import javax.inject.Inject

@RuntimePermissions
class StatusUpdateActivity: AppCompatActivity() {
    companion object {
        const val REQUEST_PHOTO_SELECT = 0
        const val ARG_REPLY_STATUS_ID = "reply_status_id"
        const val ARG_REPLY_SCREEN_NAME = "reply_screen_name"

        fun newIntent(context: Context, replyStatusId: Long? = null, replyScreenName: String? = null): Intent =
            Intent(context, StatusUpdateActivity::class.java).apply {
                putExtra(ARG_REPLY_STATUS_ID, replyStatusId)
                putExtra(ARG_REPLY_SCREEN_NAME, replyScreenName)
            }
    }

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    private var photos: Array<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.inject(this)

        setContentView(R.layout.status_update)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        setSupportActionBar(status_update_toolbar)

        val replyStatusId = intent.getLongExtraOrNull(ARG_REPLY_STATUS_ID)
        val replyScreenName = intent.getStringExtraOrNull(ARG_REPLY_SCREEN_NAME)
        replyScreenName?.let { status_input.setText(it) }

        status_input.requestFocus()
        select_photos.setOnClickListener {
            startPhotoSelectorWithPermissionCheck()
        }

        send_tweet.setOnClickListener {
            val tweet = status_input.text.toString()
            val i = StatusUpdateService.newIntent(applicationContext, tweet, replyStatusId, photos)
            this.startService(i)
            finish()
        }

        // initial photo from intent
        // TODO: check login and permission
        intent.getExtraStreamOrNull()?.let {
            val path = contentResolver.getImagePath(it as Uri)
            showSelectedPhotos(arrayOf(path))
        }

        val account = accountRepo.find(prefs.currentUserId)!!
        PicassoUtil.userIcon(account.user, toolbar_avatar)
        toolbar_screen_name.text = account.user.screenName
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun startPhotoSelector() {
        val i = Intent(this, PhotoSelectorActivity::class.java)
        startActivityForResult(i, REQUEST_PHOTO_SELECT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PHOTO_SELECT && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val paths = data.getStringArrayExtra(PhotoSelectorActivity.RESULT_SELECTED_PHOTOS)
            showSelectedPhotos(paths)
        }
    }

    private fun showSelectedPhotos(paths: Array<String>) {
        photos = null
        status_thumbnail_tile.removeAllViews()
        if (paths.isEmpty()) return

        photos = paths
        paths.forEachIndexed { _, path ->
            val imgView = AspectRatioImageView(this)
            status_thumbnail_tile.addView(imgView)
            PicassoUtil.thumbnail(path, imgView)
        }
    }
}
