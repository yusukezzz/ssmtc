package net.yusukezzz.ssmtc.data.api

import java.net.URLEncoder

object SearchQueryBuilder {
    fun build(params: Timeline): String {
        val queries = mutableListOf(params.query)

        if (!params.includeRetweets) {
            queries.add("exclude:retweets")
        }

        when (params.filter.showing) {
            ContentShowing.ANY_MEDIA -> queries.add("filter:media")
            ContentShowing.PHOTO -> queries.add("filter:images")
            ContentShowing.VIDEO -> queries.add("filter:videos")
            else -> {
                /* do nothing */
            }
        }

        return URLEncoder.encode(queries.joinToString(" "), "UTF-8")
    }
}
