package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.support.v7.app.AppCompatDialogFragment
import net.yusukezzz.ssmtc.data.api.TimelineParameter

open class TimelineSelectDialogFragment : AppCompatDialogFragment() {
    interface TimelineSelectListener {
        fun onTimelineSelected(timeline: TimelineParameter)
        fun openListsDialog()
        fun openSearchInputDialog()
        fun openUserInputDialog()
    }

    protected lateinit var listener: TimelineSelectListener


    fun setTimelineSelectListener(listener: TimelineSelectListener): TimelineSelectDialogFragment {
        this.listener = listener

        return this
    }
}
