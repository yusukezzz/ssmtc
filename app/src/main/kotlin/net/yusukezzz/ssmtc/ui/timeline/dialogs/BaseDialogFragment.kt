package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.support.v7.app.AppCompatDialogFragment
import net.yusukezzz.ssmtc.data.api.Timeline

open class BaseDialogFragment : AppCompatDialogFragment() {
    interface TimelineSelectListener {
        fun onTimelineSelect(timeline: Timeline)
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
