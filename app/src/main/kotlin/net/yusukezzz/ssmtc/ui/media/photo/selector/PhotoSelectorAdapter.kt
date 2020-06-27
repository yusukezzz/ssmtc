package net.yusukezzz.ssmtc.ui.media.photo.selector

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.util.beVisibleIf
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import java.util.*

class PhotoSelectorAdapter(
    private val photoPaths: List<Uri>,
    val listener: PhotoSelectorListener
) : RecyclerView.Adapter<PhotoSelectorAdapter.ViewHolder>() {
    companion object {
        const val MAX_PHOTO_COUNT = 4
    }

    interface PhotoSelectorListener {
        fun onSelectChanged(remainCount: Int)
        fun onSelectCompleted()
    }

    private val selected = ArrayList<Uri>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.photo_select_item, parent, false)
        return ViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) =
        holder.bindPhoto(photoPaths[pos], isSelected(photoPaths[pos]))

    override fun getItemCount(): Int = photoPaths.size

    fun selectedPhotoPaths(): ArrayList<Uri> = selected

    fun toggleSelected(uri: Uri) {
        if (!selected.remove(uri)) {
            selected.add(uri)
        }
        notifyDataSetChanged()
        listener.onSelectChanged(MAX_PHOTO_COUNT - selected.size)
        if (selected.size == MAX_PHOTO_COUNT) {
            listener.onSelectCompleted()
        }
    }

    private fun isSelected(uri: Uri): Boolean = selected.indexOf(uri) > -1

    class ViewHolder(val view: View, private val adapter: PhotoSelectorAdapter) :
        RecyclerView.ViewHolder(view) {
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        private val selectedIcon: ImageView = view.findViewById(R.id.iv_selected)

        fun bindPhoto(uri: Uri, selected: Boolean) {
            PicassoUtil.thumbnail(uri, thumbnail)
            thumbnail.setOnClickListener { adapter.toggleSelected(uri) }
            selectedIcon.beVisibleIf(selected)
        }
    }
}
