package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.model.TwList

class ListsSelectDialog : BaseDialogFragment() {
    companion object {
        val ARG_TW_LISTS = "tw_lists"

        fun newInstance(lists: List<TwList>): ListsSelectDialog = ListsSelectDialog().apply {
            arguments = Bundle().apply {
                putParcelableArray(ARG_TW_LISTS, lists.toTypedArray())
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val twLists = arguments.getParcelableArray(ARG_TW_LISTS).map { it as TwList }
        val items = twLists.map { it.fullName }.toTypedArray()

        return AlertDialog.Builder(activity).apply {
            setTitle(R.string.lists_selector_title)
            setItems(items) { _, which ->
                twLists[which].let { listener.onTimelineSelect(TimelineParameter.list(it.id, it.fullName)) }
            }
            setNegativeButton(R.string.lists_selector_cancel) { _, _ -> /* do nothing */ }
        }.create()
    }
}
