package net.yusukezzz.ssmtc.services

import nz.bradcampbell.paperparcel.PaperParcel

@PaperParcel
data class TimelineParameter(
    val type: Int,
    val title: String,
    val count: Int = TimelineParameter.MAX_RETRIEVE_COUNT,
    val sinceId: Long? = null,
    val maxId: Long? = null,
    val screenName: String? = null,
    val query: String? = null,
    val listId: Long? = null
) {
    companion object {
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

    fun next(sinceId: Long?): TimelineParameter = copy(sinceId = sinceId, maxId = null)
    fun gap(sinceId: Long, maxId: Long): TimelineParameter = copy(sinceId = sinceId, maxId = maxId)
    fun previous(maxId: Long?): TimelineParameter = copy(sinceId = null, maxId = maxId)
}

