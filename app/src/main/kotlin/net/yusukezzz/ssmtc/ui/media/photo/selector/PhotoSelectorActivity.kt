package net.yusukezzz.ssmtc.ui.media.photo.selector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.photo_selector.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity

class PhotoSelectorActivity : MediaBaseActivity(),
    PhotoSelectorAdapter.PhotoSelectorListener {
    companion object {
        const val RESULT_SELECTED_PHOTOS = "result_selected_photos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_selector)

        initView()
    }

    private fun initView() {
        val photos = findPhotosFromGallery()
        val spanCount = 3
        val space = 3
        val deco = GridSpacingItemDecoration(spanCount, space)
        photo_selector_grid.addItemDecoration(deco)
        val layoutManager = GridLayoutManager(this, spanCount)
        photo_selector_grid.layoutManager = layoutManager
        photo_selector_grid.setHasFixedSize(true)
        photo_selector_grid.adapter = PhotoSelectorAdapter(photos, this)

        photo_selector_complete.setOnClickListener { onSelectCompleted() }
    }

    private fun findPhotosFromGallery(): List<String> {
        val photos = arrayListOf<Uri>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val column = MediaStore.Images.Media.DATA
        val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"
        val cursor = contentResolver.query(uri, arrayOf(column), null, null, orderBy)
        cursor.use {
            while (it.moveToNext()) {
                photos.add(Uri.parse(it.getString(it.getColumnIndex(column))))
            }
        }

        return photos.map { it.toString() }
    }

    override fun onSelectChanged(remainCount: Int) {
        val title = photo_selector_title.text
        photo_selector_title.text = title.replace(Regex("\\d"), remainCount.toString())
    }

    override fun onSelectCompleted() {
        val i = Intent()
        val paths = (photo_selector_grid.adapter as PhotoSelectorAdapter).selectedPhotoPaths()
        i.putExtra(RESULT_SELECTED_PHOTOS, paths)
        setResult(RESULT_OK, i)
        finish()
    }
}
