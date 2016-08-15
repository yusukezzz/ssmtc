package net.yusukezzz.ssmtc.screens.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.timeline_setting.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.services.TimelineParameter
import net.yusukezzz.ssmtc.services.TimelineParameterParcel

class TimelineSettingDialog: AppCompatDialogFragment() {
    companion object {
        val ARG_TIMELINE = "timeline"

        fun newInstance(timeline: TimelineParameter): TimelineSettingDialog = TimelineSettingDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TIMELINE, TimelineParameterParcel(timeline))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeline = arguments.getParcelable<TimelineParameterParcel>(ARG_TIMELINE).data
        val view = activity.layoutInflater.inflate(R.layout.timeline_setting, null, false)

        if (timeline.type == TimelineParameter.TYPE_SEARCH) {
            view.timeline_query.visibility = View.VISIBLE
            view.timeline_query_edit.setText(timeline.query)
        }

        view.timeline_title_edit.setText(timeline.title)
        val adapter = ArrayAdapter.createFromResource(context, R.array.filter_contents, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.timeline_contents_spinner.adapter = adapter
        view.timeline_include.setText(timeline.filter.includeWords.joinToString("\n"))
        view.timeline_exclude.setText(timeline.filter.excludeWords.joinToString("\n"))

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.setting_dialog_title)
            setView(view)
            setPositiveButton(R.string.setting_dialog_ok, { dialog, which -> save() })
            setNegativeButton(R.string.setting_dialog_cancel, { d, w -> /* do nothing */ })
        }.create()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun save() {
        println("TODO: save timeline setting")
    }
}
