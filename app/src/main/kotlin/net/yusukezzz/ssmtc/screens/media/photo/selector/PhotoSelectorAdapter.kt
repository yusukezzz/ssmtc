package net.yusukezzz.ssmtc.screens.media.photo.selector

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.R
import java.io.File
import java.util.*

class PhotoSelectorAdapter(
    val photoPaths: List<String>,
    val listener: PhotoSelectorListener
): RecyclerView.Adapter<PhotoSelectorAdapter.ViewHolder>() {
    companion object {
        val MAX_PHOTO_COUNT = 4
    }

    interface PhotoSelectorListener {
        fun onSelectChanged(remainCount: Int)
        fun onSelectCompleted()
    }

    private val selected = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_select_item, parent, false)
        return ViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindPhoto(photoPaths[position], isSelected(photoPaths[position]))

    override fun getItemCount(): Int = photoPaths.size

    fun selectedPhotoPaths(): Array<String> = selected.toTypedArray()

    fun toggleSelected(path: String) {
        if (!selected.remove(path)) {
            selected.add(path)
        }
        notifyDataSetChanged()
        listener.onSelectChanged(MAX_PHOTO_COUNT - selected.size)
        if (selected.size == MAX_PHOTO_COUNT) {
            listener.onSelectCompleted()
        }
    }

    fun isSelected(path: String): Boolean = selected.indexOf(path) > -1

    class ViewHolder(val view: View, val adapter: PhotoSelectorAdapter): RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail) as ImageView
        val selectedIcon: ImageView = view.findViewById(R.id.iv_selected) as ImageView

        fun bindPhoto(path: String, selected: Boolean) {
            Picasso.with(view.context)
                .load(File(path)).fit().centerCrop().into(thumbnail)
            thumbnail.setOnClickListener { adapter.toggleSelected(path) }
            if (selected) {
                selectedIcon.visibility = View.VISIBLE
            } else {
                selectedIcon.visibility = View.GONE
            }
        }
    }
}
