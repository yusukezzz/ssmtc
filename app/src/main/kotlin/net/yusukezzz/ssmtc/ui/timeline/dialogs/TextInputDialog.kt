package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.util.resolveAttributeId

class TextInputDialog : BaseDialogFragment() {
    companion object {
        val ARG_TITLE = "title"
        val ARG_TIMELINE_TYPE = "type"

        fun newInstance(type: Int, title: Int): TextInputDialog = TextInputDialog().apply {
            arguments = Bundle().apply {
                putInt(ARG_TIMELINE_TYPE, type)
                putInt(ARG_TITLE, title)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments.getInt(ARG_TITLE)
        val type = arguments.getInt(ARG_TIMELINE_TYPE)

        val edit = EditText(context)
        val padding = resources.getDimensionPixelSize(context.resolveAttributeId(android.R.attr.dialogPreferredPadding))
        val linear = LinearLayout(context)
        linear.setPadding(padding, padding, padding, padding)
        linear.orientation = LinearLayout.VERTICAL
        linear.addView(edit)

        val dialog = AlertDialog.Builder(context).apply {
            setTitle(title)
            setView(linear)
            setPositiveButton(R.string.input_dialog_ok, { _, _ ->
                val input = edit.text.toString().trim()
                val timeline = when (type) {
                    TimelineParameter.TYPE_SEARCH -> TimelineParameter.search(input)
                    TimelineParameter.TYPE_USER -> TimelineParameter.user(input)
                    else -> throw RuntimeException("unknown timeline type: $type")
                }
                listener.onTimelineSelect(timeline)
            })
            setNegativeButton(R.string.input_dialog_cancel, { _, _ -> /* do nothing */ })
        }.create()

        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return dialog
    }
}
