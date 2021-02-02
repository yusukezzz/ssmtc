package net.yusukezzz.ssmtc.ui.status.update

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.repository.SsmtcAccountRepository
import net.yusukezzz.ssmtc.databinding.StatusUpdateBinding
import net.yusukezzz.ssmtc.ui.media.photo.selector.PhotoSelectorActivity
import net.yusukezzz.ssmtc.ui.misc.AspectRatioImageView
import net.yusukezzz.ssmtc.util.*
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import javax.inject.Inject

@RuntimePermissions
class StatusUpdateActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_PHOTO_SELECT = 0
        const val ARG_REPLY_STATUS_ID = "reply_status_id"
        const val ARG_REPLY_SCREEN_NAME = "reply_screen_name"

        fun newIntent(
            context: Context,
            replyStatusId: Long? = null,
            replyScreenName: String? = null
        ): Intent =
            Intent(context, StatusUpdateActivity::class.java).apply {
                putExtra(ARG_REPLY_STATUS_ID, replyStatusId)
                putExtra(ARG_REPLY_SCREEN_NAME, replyScreenName)
            }
    }

    @Inject
    lateinit var prefs: Preferences

    @Inject
    lateinit var accountRepo: SsmtcAccountRepository

    private var photos: Array<Uri>? = null

    private lateinit var binding: StatusUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Application.component.inject(this)

        binding = StatusUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        setSupportActionBar(binding.statusUpdateToolbar)

        val replyStatusId = intent.getLongExtraOrNull(ARG_REPLY_STATUS_ID)
        val replyScreenName = intent.getStringExtraOrNull(ARG_REPLY_SCREEN_NAME)
        replyScreenName?.let { binding.statusInput.setText("@$it ") }

        binding.apply {
            statusInput.requestFocus()
            selectPhotos.setOnClickListener {
                startPhotoSelectorWithPermissionCheck()
            }
            sendTweet.setOnClickListener {
                val tweet = statusInput.text.toString()
                val i = StatusUpdateService.newIntent(applicationContext, tweet, replyStatusId, photos)
                startService(i)
                finish()
            }
            val account = accountRepo.find(prefs.currentUserId)!!
            PicassoUtil.userIcon(account.user, toolbarAvatar)
            toolbarScreenName.text = account.user.screenName
        }

        handleIntent(intent)
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun startPhotoSelector() {
        val i = Intent(this, PhotoSelectorActivity::class.java)
        startActivityForResult(i, REQUEST_PHOTO_SELECT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO_SELECT && resultCode == RESULT_OK && data != null) {
            data.getParcelableArrayListExtra<Uri>(PhotoSelectorActivity.RESULT_SELECTED_PHOTOS)?.let {
                showSelectedPhotos(it.toTypedArray())
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) {
            return
        }
        if (!intent.isImage() && !intent.isVideo()) {
            return
        }

        // initial photo from intent
        // TODO: check login and permission
        (intent.getExtraStreamOrNull() as? Uri)?.let {
            println(it.toString())
            val mimeType: String? = contentResolver.getType(it)
            if (mimeType != null) {
                // TODO: save photos to avoid SecurityException
                showSelectedPhotos(arrayOf(getContentUri(it, mimeType)))
            }
        }
    }

    private fun showSelectedPhotos(paths: Array<Uri>) {
        photos = null
        binding.statusThumbnailTile.removeAllViews()
        if (paths.isEmpty()) return

        photos = paths
        paths.forEachIndexed { _, path ->
            val imgView = AspectRatioImageView(this)
            binding.statusThumbnailTile.addView(imgView)
            PicassoUtil.thumbnail(path, imgView)
        }
    }

    private fun getContentUri(content: Uri, mimeType: String): Uri {
        val id = BaseColumns._ID
        val contentUri = when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> throw RuntimeException(
                "unknown mimeType or not content uri: uri=$content mimeType=$mimeType"
            )
        }
        val projections = arrayOf(id)
        val selection = null
        val args = null
        val order = null
        val uri: Uri? = contentResolver.query(content, projections, selection, args, order)?.use {
            it.moveToFirst()
            try {
                ContentUris.withAppendedId(contentUri, it.getLong(it.getColumnIndex(id)))
            } catch (e: Exception) {
                null
            }
        }
        return uri ?: content
    }
}
