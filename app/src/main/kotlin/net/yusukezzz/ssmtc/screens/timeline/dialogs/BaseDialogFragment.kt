package net.yusukezzz.ssmtc.screens.timeline.dialogs

import android.support.v7.app.AppCompatDialogFragment
import net.yusukezzz.ssmtc.services.TimelineParameter

open class BaseDialogFragment: AppCompatDialogFragment() {
    interface TimelineSelectListener {
        fun onTimelineSelected(timeline: TimelineParameter)
        fun openListsDialog()
        fun openSearchInputDialog()
        fun openUserInputDialog()
    }

    protected lateinit var listener: TimelineSelectListener


    fun setTimelineSelectListener(listener: TimelineSelectListener): BaseDialogFragment {
        this.listener = listener

        return this
    }
}
