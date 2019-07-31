package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import net.yusukezzz.ssmtc.data.api.Timeline

class ConfirmTimelineSelectDialog : BaseDialogFragment() {

    companion object {
        const val ARG_TIMELINE = "timeline"

        fun newInstance(timeline: Timeline): ConfirmTimelineSelectDialog = ConfirmTimelineSelectDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TIMELINE, timeline)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeline: Timeline = arguments!!.getParcelable(ARG_TIMELINE)

        return AlertDialog.Builder(context!!).apply {
            setMessage("${timeline.title} タイムラインを追加しますか？")
            setPositiveButton("追加") { _, _ -> listener.onTimelineSelect(timeline) }
            setNegativeButton("戻る") { _, _ -> /* do nothing */ }
        }.create()
    }
}