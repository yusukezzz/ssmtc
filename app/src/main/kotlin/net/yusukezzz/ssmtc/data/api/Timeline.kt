package net.yusukezzz.ssmtc.data.api

import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@PaperParcel
data class Timeline(
    val uuid: UUID,
    val type: Int,
    val title: String,
    val filter: FilterRule = FilterRule.default(),
    val screenName: String? = null,
    val query: String? = null,
    val listId: Long? = null,
    val includeRetweets: Boolean = true
) : PaperParcelable, Comparable<Timeline> {
    override fun compareTo(other: Timeline): Int = compareValuesBy(this, other, { it.type }, { it.title.toLowerCase() })

    companion object {
        @JvmField val CREATOR = PaperParcelTimeline.CREATOR
        val TYPE_HOME = 0
        val TYPE_MENTIONS = 1
        val TYPE_LISTS = 2
        val TYPE_SEARCH = 3
        val TYPE_USER = 4

        // factory
        fun home(): Timeline = Timeline(UUID.randomUUID(), TYPE_HOME, "Home")

        fun mentions(): Timeline = Timeline(UUID.randomUUID(), TYPE_MENTIONS, "Mentions")
        fun list(listId: Long, slug: String) = Timeline(uuid = UUID.randomUUID(), type = TYPE_LISTS, title = slug, listId = listId)
        fun search(query: String) = Timeline(uuid = UUID.randomUUID(), type = TYPE_SEARCH, title = query, query = query)
        fun user(screenName: String) = Timeline(uuid = UUID.randomUUID(), type = TYPE_USER, title = screenName, screenName = screenName)
    }
}

