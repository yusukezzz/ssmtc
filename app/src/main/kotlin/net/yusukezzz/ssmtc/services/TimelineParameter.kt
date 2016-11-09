package net.yusukezzz.ssmtc.services

import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable
import kotlin.comparisons.compareValuesBy

@PaperParcel
data class TimelineParameter(
    val type: Int,
    val title: String,
    val count: Int = TimelineParameter.MAX_RETRIEVE_COUNT,
    val filter: TimelineFilter = TimelineFilter.default(),
    val screenName: String? = null,
    val query: String? = null,
    val listId: Long? = null,
    val includeRetweets: Boolean = true
) : PaperParcelable, Comparable<TimelineParameter> {
    override fun compareTo(other: TimelineParameter): Int = compareValuesBy(this, other, { it.type }, { it.title })

    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(TimelineParameter::class.java)

        val MAX_RETRIEVE_COUNT = 50

        val TYPE_HOME = 0
        val TYPE_MENTIONS = 1
        val TYPE_LISTS = 2
        val TYPE_SEARCH = 3
        val TYPE_USER = 4

        fun home(): TimelineParameter = TimelineParameter(TYPE_HOME, "Home")
        fun mentions(): TimelineParameter = TimelineParameter(TYPE_MENTIONS, "Mentions")
        fun list(listId: Long, slug: String) = TimelineParameter(type = TYPE_LISTS, title = slug, listId = listId)
        fun search(query: String) = TimelineParameter(type = TYPE_SEARCH, title = query, query = query)
        fun user(screenName: String) = TimelineParameter(type = TYPE_USER, title = screenName, screenName = screenName)
    }
}

