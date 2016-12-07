package net.yusukezzz.ssmtc.ui.status.update

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.status_update.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.ui.media.photo.selector.PhotoSelectorActivity
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.RoundedTransformation
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class StatusUpdateActivity: AppCompatActivity() {
    companion object {
        val REQUEST_PHOTO_SELECT = 0
        val ARG_REPLY_STATUS_ID = "reply_status_id"
        val ARG_REPLY_SCREEN_NAME = "reply_screen_name"

        fun newIntent(context: Context, replyStatusId: Long? = null, replyScreenName: String? = null): Intent =
            Intent(context, StatusUpdateActivity::class.java).apply {
                putExtra(ARG_REPLY_STATUS_ID, replyStatusId)
                putExtra(ARG_REPLY_SCREEN_NAME, replyScreenName)
            }
    }

    private var photos: Array<String>? = null
    private val inflater by lazy { LayoutInflater.from(this) }
    private val mediaViewIdLists: List<List<Int>> = listOf(
        listOf(R.id.media_photo_single),
        listOf(R.id.media_photo_two_1, R.id.media_photo_two_2),
        listOf(R.id.media_photo_three_1, R.id.media_photo_three_2, R.id.media_photo_three_3),
        listOf(R.id.media_photo_four_1, R.id.media_photo_four_2, R.id.media_photo_four_3, R.id.media_photo_four_4)
    )
    private val mediaLayoutIds: List<Int> by lazy {
        listOf(
            R.layout.media_photo_single,
            R.layout.media_photo_two,
            R.layout.media_photo_three,
            R.layout.media_photo_four
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.status_update)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        setSupportActionBar(status_update_toolbar)

        val replyStatusId = intent.getLongExtraOrNull(ARG_REPLY_STATUS_ID)
        val replyScreenName = intent.getStringExtraOrNull(ARG_REPLY_SCREEN_NAME)
        replyScreenName?.let { status_input.setText("@${it} ") }

        status_input.requestFocus()
        select_photos.setOnClickListener {
            StatusUpdateActivityPermissionsDispatcher.startPhotoSelectorWithCheck(this)
        }

        send_tweet.setOnClickListener {
            val tweet = status_input.text.toString()
            val i = StatusUpdateService.newIntent(this, tweet, replyStatusId, photos)
            this.startService(i)
            finish()
        }

        // initial photo from intent
        // TODO: check login and permission
        intent.getExtraStreamOrNull()?.let {
            val path = contentResolver.getImagePath(it as Uri)
            showSelectedPhotos(arrayOf(path))
        }

        val account = PreferencesHolder.prefs.currentAccount!!
        Picasso.with(this).load(account.user.profileImageUrl)
            .transform(RoundedTransformation(8))
            .centerCrop().fit()
            .into(toolbar_avatar)
        toolbar_screen_name.text = account.user.screenName
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun startPhotoSelector() {
        val i = Intent(this, PhotoSelectorActivity::class.java)
        startActivityForResult(i, REQUEST_PHOTO_SELECT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        StatusUpdateActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PHOTO_SELECT && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val paths = data.getStringArrayExtra(PhotoSelectorActivity.RESULT_SELECTED_PHOTOS)
            showSelectedPhotos(paths)
        }
    }

    fun showSelectedPhotos(paths: Array<String>) {
        photos = null
        tweet_media_container.removeAllViews()
        if (paths.isEmpty()) return

        photos = paths
        val imgNum = paths.size
        val template = inflater.inflate(mediaLayoutIds[imgNum - 1], null)
        paths.forEachIndexed { i, path ->
            val imgView = template.findViewById(mediaViewIdLists[imgNum - 1][i]) as ImageView
            Picasso.with(this).load(File(path))
                .fit().centerCrop().into(imgView)
        }
        tweet_media_container.addView(template)
    }
}
