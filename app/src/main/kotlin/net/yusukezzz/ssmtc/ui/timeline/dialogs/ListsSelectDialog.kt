package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.model.TwList

class ListsSelectDialog : BaseDialogFragment() {
    companion object {
        const val ARG_TW_LISTS = "tw_lists"

        fun newInstance(lists: List<TwList>): ListsSelectDialog = ListsSelectDialog().apply {
            arguments = Bundle().apply {
                putParcelableArray(ARG_TW_LISTS, lists.toTypedArray())
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val twLists = arguments?.getParcelableArray(ARG_TW_LISTS)?.map { it as TwList }
        val items = twLists?.map { it.fullName }?.toTypedArray()

        return AlertDialog.Builder(context!!).apply {
            setTitle(R.string.lists_selector_title)
            setItems(items) { _, which ->
                twLists?.get(which)
                    ?.let { listener.onTimelineSelect(Timeline.list(it.id, it.fullName)) }
            }
            setNegativeButton(R.string.lists_selector_cancel) { _, _ -> /* do nothing */ }
        }.create()
    }
}
