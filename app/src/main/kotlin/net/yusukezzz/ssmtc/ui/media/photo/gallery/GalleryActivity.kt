package net.yusukezzz.ssmtc.ui.media.photo.gallery

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.view.ViewPager
import kotlinx.android.synthetic.main.photo_gallery.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.data.api.model.MediaParcel
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity
import net.yusukezzz.ssmtc.util.toast
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class GalleryActivity : MediaBaseActivity(), ViewPager.OnPageChangeListener {
    companion object {
        val ARG_IMAGES = "images"
        val ARG_START_POSITION = "start_position"

        fun newIntent(context: Context, images: List<Media>, pos: Int): Intent =
            Intent(context, GalleryActivity::class.java).apply {
                putExtra(ARG_IMAGES, images.map(::MediaParcel).toTypedArray())
                putExtra(ARG_START_POSITION, pos)
            }
    }

    val images: List<Media> by lazy { intent.getParcelableArrayExtra(ARG_IMAGES).map { (it as MediaParcel).data } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_gallery)

        val startPos = intent.getIntExtra(ARG_START_POSITION, 0)
        val adapter = GalleryPageAdapter(applicationContext, images)

        gallery.adapter = adapter
        gallery.currentItem = startPos

        photo_gallery_download.setOnClickListener {
            GalleryActivityPermissionsDispatcher.downloadImageWithCheck(this)
        }

        photo_gallery_share.setOnClickListener {
            shareImage()
        }

        if (images.size > 1) {
            setCurrentPage(startPos)
            gallery.addOnPageChangeListener(this)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        // do nothing
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // do nothing
    }

    override fun onPageSelected(position: Int) = setCurrentPage(position)

    private fun setCurrentPage(index: Int) {
        val page = index + 1
        val max = images.size
        currentPage.text = "$page/$max"
    }


    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun downloadImage() {
        val media = images[gallery.currentItem]
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val uri = Uri.parse(media.orig_url)
        val ext = media.media_url.split(".").last()
        val formatter = DateTimeFormatter.ofPattern("YMd_Hms")
        val filename = LocalDateTime.now().format(formatter) + "." + ext

        val req = DownloadManager.Request(uri)
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, resources.getString(R.string.app_name) + "/" + filename)

        manager.enqueue(req)
        toast(R.string.photo_download_start)
    }

    private fun shareImage() {
        val media = images[gallery.currentItem]
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, media.media_url) // default size url
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        GalleryActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults)
    }
}
