package net.yusukezzz.ssmtc.ui.media.photo.selector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import kotlinx.android.synthetic.main.photo_selector.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity

class PhotoSelectorActivity: MediaBaseActivity(),
    PhotoSelectorAdapter.PhotoSelectorListener {
    companion object {
        val RESULT_SELECTED_PHOTOS = "result_selected_photos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_selector)

        initView()
    }

    fun initView() {
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

    fun findPhotosFromGallery(): List<String> {
        val photos = arrayListOf<Uri>()
        val columns = arrayOf(MediaStore.Images.Media.DATA)
        val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy)
        cursor.use {
            while (it.moveToNext()) {
                photos.add(Uri.parse(it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))))
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
        setResult(AppCompatActivity.RESULT_OK, i)
        finish()
    }
}