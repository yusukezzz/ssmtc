package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.timeline_setting.view.*
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.FilterRule
import net.yusukezzz.ssmtc.data.api.Timeline
import javax.inject.Inject

class TimelineSettingDialog : AppCompatDialogFragment() {
    companion object {
        const val ARG_TIMELINE = "timeline"

        fun newInstance(timeline: Timeline): TimelineSettingDialog = TimelineSettingDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TIMELINE, timeline)
            }
        }
    }

    interface TimelineSettingListener {
        fun onSaveTimeline(timeline: Timeline)
    }

    @Inject
    lateinit var prefs: Preferences

    private lateinit var listener: TimelineSettingListener

    fun setTimelineSettingListener(listener: TimelineSettingListener): TimelineSettingDialog {
        this.listener = listener

        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Application.component.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeline: Timeline? = arguments?.getParcelable(ARG_TIMELINE)
        val view = activity!!.layoutInflater.inflate(R.layout.timeline_setting, null, false)

        if (timeline?.type == Timeline.TYPE_SEARCH) {
            view.timeline_query.visibility = View.VISIBLE
            view.timeline_query_edit.setText(timeline.query)
        }

        val adapter =
            ArrayAdapter.createFromResource(context, R.array.filter_media, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.timeline_media_spinner.adapter = adapter
        timeline?.let {
            view.timeline_title_edit.setText(it.title)
            view.include_rts.isChecked = it.includeRetweets
            view.timeline_media_spinner.setSelection(FilterRule.Showing.values().indexOf(it.filter.showing))
            view.timeline_include.setText(it.filter.includeWords.joinToString("\n"))
            view.timeline_exclude.setText(it.filter.excludeWords.joinToString("\n"))
        }

        return AlertDialog.Builder(context!!).apply {
            setTitle(R.string.setting_dialog_title)
            setView(view)
            setPositiveButton(R.string.setting_dialog_ok) { _, _ -> timeline?.let { save(it, view) } }
            setNegativeButton(R.string.setting_dialog_cancel) { _, _ -> /* do nothing */ }
        }.create()
    }

    private fun save(oldTimeline: Timeline, view: View) {
        val newTitle = view.timeline_title_edit.text.toString().trim()
        val newQuery = view.timeline_query_edit.text.toString().trim()
        val includeRts = view.include_rts.isChecked

        val pos = view.timeline_media_spinner.selectedItemPosition
        val showing = FilterRule.Showing.values()[pos]
        val includeWords = view.timeline_include.text.toString().lines().filter(String::isNotEmpty)
        val excludeWords = view.timeline_exclude.text.toString().lines().filter(String::isNotEmpty)
        val newFilter = FilterRule(showing, includeWords, excludeWords)

        val newTimeline =
            oldTimeline.copy(title = newTitle, query = newQuery, filter = newFilter, includeRetweets = includeRts)
        listener.onSaveTimeline(newTimeline)
    }
}
