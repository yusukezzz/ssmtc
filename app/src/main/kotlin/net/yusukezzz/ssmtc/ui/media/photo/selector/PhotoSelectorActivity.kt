package net.yusukezzz.ssmtc.ui.media.photo.selector

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.recyclerview.widget.GridLayoutManager
import net.yusukezzz.ssmtc.databinding.PhotoSelectorBinding
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity

class PhotoSelectorActivity : MediaBaseActivity(),
    PhotoSelectorAdapter.PhotoSelectorListener {
    companion object {
        const val RESULT_SELECTED_PHOTOS = "result_selected_photos"
    }

    private lateinit var binding: PhotoSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PhotoSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        val photos = findPhotosFromGallery()
        val spanCount = 3
        val space = 3
        val deco = GridSpacingItemDecoration(spanCount, space)
        val layoutManager = GridLayoutManager(this, spanCount)
        val adapter = PhotoSelectorAdapter(photos, this)
        binding.apply {
            photoSelectorGrid.addItemDecoration(deco)
            photoSelectorGrid.layoutManager = layoutManager
            photoSelectorGrid.setHasFixedSize(true)
            photoSelectorGrid.adapter = adapter

            photoSelectorComplete.setOnClickListener { onSelectCompleted() }
        }
    }

    private fun findPhotosFromGallery(): List<Uri> {
        val photos = arrayListOf<Uri>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val column = MediaStore.Images.Media._ID
        val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"
        contentResolver.query(uri, arrayOf(column), null, null, orderBy)?.use {
            while (it.moveToNext()) {
                photos.add(ContentUris.withAppendedId(uri, it.getLong(it.getColumnIndex(column))))
            }
        }

        return photos
    }

    override fun onSelectChanged(remainCount: Int) {
        val title = binding.photoSelectorTitle.text
        binding.photoSelectorTitle.text = title.replace(Regex("\\d"), remainCount.toString())
    }

    override fun onSelectCompleted() {
        val i = Intent()
        val paths = (binding.photoSelectorGrid.adapter as PhotoSelectorAdapter).selectedPhotoPaths()
        i.putExtra(RESULT_SELECTED_PHOTOS, paths)
        setResult(RESULT_OK, i)
        finish()
    }
}
