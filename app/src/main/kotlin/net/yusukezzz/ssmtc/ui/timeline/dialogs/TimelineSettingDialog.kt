package net.yusukezzz.ssmtc.ui.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import net.yusukezzz.ssmtc.Application
import net.yusukezzz.ssmtc.Preferences
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.ContentShowing
import net.yusukezzz.ssmtc.data.api.FilterRule
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.databinding.TimelineSettingBinding
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
    private lateinit var view: TimelineSettingBinding

    fun setTimelineSettingListener(listener: TimelineSettingListener): TimelineSettingDialog {
        this.listener = listener

        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Application.component.inject(this)
        view = TimelineSettingBinding.inflate(layoutInflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeline: Timeline? = arguments?.getParcelable(ARG_TIMELINE)

        if (timeline?.type == Timeline.TYPE_SEARCH) {
            view.timelineQuery.visibility = View.VISIBLE
            view.timelineQueryEdit.setText(timeline.query)
        }

        val adapter =
            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.filter_media,
                android.R.layout.simple_spinner_item
            )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.timelineMediaSpinner.adapter = adapter
        timeline?.let {
            view.timelineTitleEdit.setText(it.title)
            view.includeRts.isChecked = it.includeRetweets
            view.timelineMediaSpinner.setSelection(
                ContentShowing.values().indexOf(it.filter.showing)
            )
            view.timelineInclude.setText(it.filter.includeWords.joinToString("\n"))
            view.timelineExclude.setText(it.filter.excludeWords.joinToString("\n"))
        }

        return AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.setting_dialog_title)
            setView(view.root)
            setPositiveButton(R.string.setting_dialog_ok) { _, _ ->
                timeline?.let {
                    save(it, view)
                }
            }
            setNegativeButton(R.string.setting_dialog_cancel) { _, _ -> /* do nothing */ }
        }.create()
    }

    private fun save(oldTimeline: Timeline, value: TimelineSettingBinding) {
        val newTitle = value.timelineTitleEdit.text.toString().trim()
        val newQuery = view.timelineQueryEdit.text.toString().trim()
        val includeRts = view.includeRts.isChecked

        val pos = view.timelineMediaSpinner.selectedItemPosition
        val showing = ContentShowing.values()[pos]
        val includeWords = value.timelineInclude.text.toString().lines().filter(String::isNotEmpty)
        val excludeWords = value.timelineExclude.text.toString().lines().filter(String::isNotEmpty)
        val newFilter = FilterRule(showing, includeWords, excludeWords)

        val newTimeline =
            oldTimeline.copy(
                title = newTitle,
                query = newQuery,
                filter = newFilter,
                includeRetweets = includeRts
            )
        listener.onSaveTimeline(newTimeline)
    }
}
