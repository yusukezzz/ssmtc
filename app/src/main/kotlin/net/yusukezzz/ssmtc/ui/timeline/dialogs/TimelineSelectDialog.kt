package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.TimelineParameter


class TimelineSelectDialog : BaseDialogFragment() {
    companion object {
        val TIMELINE_TYPES = arrayOf("Home", "Mentions", "Lists", "Search", "User")

        fun newInstance(): TimelineSelectDialog = TimelineSelectDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity).apply {
            setTitle(R.string.timeline_selector_title)
            setItems(TIMELINE_TYPES) { dialog, which ->
                when (which) {
                    TimelineParameter.TYPE_HOME -> listener.onTimelineSelect(TimelineParameter.home())
                    TimelineParameter.TYPE_MENTIONS -> listener.onTimelineSelect(TimelineParameter.mentions())
                    TimelineParameter.TYPE_LISTS -> listener.onListsSelectorOpen()
                    TimelineParameter.TYPE_SEARCH -> listener.onSearchInputOpen()
                    TimelineParameter.TYPE_USER -> listener.onScreenNameInputOpen()
                }
            }
            setNegativeButton(R.string.timeline_selector_cancel) { d, w -> /* do nothing */ }
        }.create()
    }
}

