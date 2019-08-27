package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.Timeline

class TimelineSelectDialog : BaseDialogFragment() {
    companion object {
        val TIMELINE_TYPES = arrayOf("Home", "Mentions", "Lists", "Search", "User")

        fun newInstance(): TimelineSelectDialog = TimelineSelectDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!).apply {
            setTitle(R.string.timeline_selector_title)
            setItems(TIMELINE_TYPES) { _, which ->
                when (which) {
                    Timeline.TYPE_HOME -> listener.onTimelineSelect(Timeline.home())
                    Timeline.TYPE_MENTIONS -> listener.onTimelineSelect(Timeline.mentions())
                    Timeline.TYPE_LISTS -> listener.onListsSelectorOpen()
                    Timeline.TYPE_SEARCH -> listener.onSearchInputOpen()
                    Timeline.TYPE_USER -> listener.onScreenNameInputOpen()
                }
            }
            setNegativeButton(R.string.timeline_selector_cancel) { _, _ -> /* do nothing */ }
        }.create()
    }
}
