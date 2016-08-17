package net.yusukezzz.ssmtc.screens.timeline.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.services.TimelineParameter

class TextInputDialog: BaseDialogFragment() {
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

        val dialog = AlertDialog.Builder(context).apply {
            setTitle(title)
            setView(edit) // TODO: use layout
            setPositiveButton(R.string.input_dialog_ok, { dialog, which ->
                val input = edit.text.toString().trim()
                val timeline = when (type) {
                    TimelineParameter.TYPE_SEARCH -> TimelineParameter.search(input)
                    TimelineParameter.TYPE_USER -> TimelineParameter.user(input)
                    else -> throw RuntimeException("unknown timeline type: $type")
                }
                listener.onTimelineSelected(timeline)
            })
            setNegativeButton(R.string.input_dialog_cancel, { d, w -> /* do nothing */ })
        }.create()

        dialog.setOnShowListener {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(edit, 0)
        }

        return dialog
    }
}
