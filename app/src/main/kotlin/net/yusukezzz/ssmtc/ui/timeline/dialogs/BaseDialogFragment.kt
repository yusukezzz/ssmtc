package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.support.v7.app.AppCompatDialogFragment
import net.yusukezzz.ssmtc.data.api.TimelineParameter

open class BaseDialogFragment : AppCompatDialogFragment() {
    interface TimelineSelectListener {
        fun onTimelineSelect(timeline: TimelineParameter)
        fun onListsSelectorOpen()
        fun onSearchInputOpen()
        fun onScreenNameInputOpen()
    }

    protected lateinit var listener: TimelineSelectListener


    fun setTimelineSelectListener(listener: TimelineSelectListener): BaseDialogFragment {
        this.listener = listener

        return this
    }
}
