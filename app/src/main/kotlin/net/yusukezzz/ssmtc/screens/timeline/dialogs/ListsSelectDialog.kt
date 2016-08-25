package net.yusukezzz.ssmtc.screens.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.json.TwList
import net.yusukezzz.ssmtc.services.TimelineParameter

class ListsSelectDialog: BaseDialogFragment() {
    companion object {
        val ARG_TW_LISTS = "tw_lists"

        fun newInstance(lists: List<TwList>): ListsSelectDialog = ListsSelectDialog().apply {
            arguments = Bundle().apply {
                putParcelableArray(ARG_TW_LISTS, lists.toTypedArray())
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val twLists: List<TwList> = arguments.getParcelableArrayList(ARG_TW_LISTS)
        val items = twLists.map { it.fullName }.toTypedArray()

        return AlertDialog.Builder(activity).apply {
            setTitle(R.string.lists_selector_title)
            setItems(items) { dialog, which ->
                val twList = twLists[which]
                listener.onTimelineSelected(TimelineParameter.list(twList.id, twList.fullName))
            }
            setNegativeButton(R.string.lists_selector_cancel) { d, w -> /* do nothing */ }
        }.create()
    }
}
