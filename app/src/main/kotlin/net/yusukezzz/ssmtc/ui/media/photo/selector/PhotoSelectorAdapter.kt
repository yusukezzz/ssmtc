package net.yusukezzz.ssmtc.ui.media.photo.selector

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.util.beVisibleIf
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import java.util.*

class PhotoSelectorAdapter(
    private val photoPaths: List<String>,
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

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) = holder.bindPhoto(photoPaths[pos], isSelected(photoPaths[pos]))

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

    private fun isSelected(path: String): Boolean = selected.indexOf(path) > -1

    class ViewHolder(val view: View, private val adapter: PhotoSelectorAdapter) : RecyclerView.ViewHolder(view) {
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        private val selectedIcon: ImageView = view.findViewById(R.id.iv_selected)

        fun bindPhoto(path: String, selected: Boolean) {
            PicassoUtil.thumbnail(path, thumbnail)
            thumbnail.setOnClickListener { adapter.toggleSelected(path) }
            selectedIcon.beVisibleIf(selected)
        }
    }
}
