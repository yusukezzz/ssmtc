package net.yusukezzz.ssmtc.data.api

import org.apache.commons.codec.net.URLCodec

object SearchQueryBuilder {
    private val urlencoder = URLCodec("UTF-8")

    fun build(params: Timeline): String {
        val queries = mutableListOf(params.query)

        if (!params.includeRetweets) {
            queries.add("exclude:retweets")
        }

        when (params.filter.showing) {
            FilterRule.Showing.ANY_MEDIA -> queries.add("filter:media")
            FilterRule.Showing.PHOTO -> queries.add("filter:images")
            FilterRule.Showing.VIDEO -> queries.add("filter:videos")
            else -> {
                /* do nothing */
            }
        }

        return urlencoder.encode(queries.joinToString(" "))
    }
}
