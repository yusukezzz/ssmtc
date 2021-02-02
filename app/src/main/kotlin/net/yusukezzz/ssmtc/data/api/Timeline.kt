package net.yusukezzz.ssmtc.data.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Timeline(
    val uuid: UUID,
    val type: Int,
    val title: String,
    val filter: FilterRule = FilterRule.default(),
    val screenName: String? = null,
    val query: String? = null,
    val listId: Long? = null,
    val includeRetweets: Boolean = true
) : Parcelable, Comparable<Timeline> {
    override fun compareTo(other: Timeline): Int =
        compareValuesBy(this, other, { it.type }, { it.title.toLowerCase() })

    companion object {
        const val TYPE_HOME = 0
        const val TYPE_MENTIONS = 1
        const val TYPE_LISTS = 2
        const val TYPE_SEARCH = 3
        const val TYPE_USER = 4

        // factory
        fun home(): Timeline = Timeline(UUID.randomUUID(), TYPE_HOME, "Home")

        fun mentions(): Timeline = Timeline(UUID.randomUUID(), TYPE_MENTIONS, "Mentions")
        fun list(listId: Long, slug: String) =
            Timeline(uuid = UUID.randomUUID(), type = TYPE_LISTS, title = slug, listId = listId)

        fun search(query: String) =
            Timeline(uuid = UUID.randomUUID(), type = TYPE_SEARCH, title = query, query = query)

        fun user(screenName: String) =
            Timeline(
                uuid = UUID.randomUUID(),
                type = TYPE_USER,
                title = screenName,
                screenName = screenName
            )
    }
}
