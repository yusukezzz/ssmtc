package net.yusukezzz.ssmtc.data.api

import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@PaperParcel
data class TimelineParameter(
    val uuid: UUID,
    val type: Int,
    val title: String,
    val count: Int = TimelineParameter.MAX_RETRIEVE_COUNT,
    val filter: FilterRule = FilterRule.default(),
    val screenName: String? = null,
    val query: String? = null,
    val listId: Long? = null,
    val includeRetweets: Boolean = true
) : PaperParcelable, Comparable<TimelineParameter> {
    override fun compareTo(other: TimelineParameter): Int = compareValuesBy(this, other, { it.type }, { it.title.toLowerCase() })

    companion object {
        @JvmField val CREATOR = PaperParcelTimelineParameter.CREATOR

        val MAX_RETRIEVE_COUNT = 50

        val TYPE_HOME = 0
        val TYPE_MENTIONS = 1
        val TYPE_LISTS = 2
        val TYPE_SEARCH = 3
        val TYPE_USER = 4

        // factory
        fun home(): TimelineParameter = TimelineParameter(UUID.randomUUID(), TYPE_HOME, "Home")

        fun mentions(): TimelineParameter = TimelineParameter(UUID.randomUUID(), TYPE_MENTIONS, "Mentions")
        fun list(listId: Long, slug: String) = TimelineParameter(uuid = UUID.randomUUID(), type = TYPE_LISTS, title = slug, listId = listId)
        fun search(query: String) = TimelineParameter(uuid = UUID.randomUUID(), type = TYPE_SEARCH, title = query, query = query)
        fun user(screenName: String) = TimelineParameter(uuid = UUID.randomUUID(), type = TYPE_USER, title = screenName, screenName = screenName)
    }
}

