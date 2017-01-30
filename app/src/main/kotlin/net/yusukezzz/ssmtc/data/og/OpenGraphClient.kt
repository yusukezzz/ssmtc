package net.yusukezzz.ssmtc.data.og

import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.Request

class OpenGraphClient {
    companion object {
        const val FAVICON_BASE_URL = "http://www.google.com/s2/favicons?domain="
        const val MAX_CACHE_SIZE = 20
    }

    private val okhttp = OkHttpClient.Builder()
        .followRedirects(true)
        .build()
    private val cache: LruCache<String, OpenGraph> by lazy { LruCache<String, OpenGraph>(MAX_CACHE_SIZE) }

    fun load(url: String): OpenGraph {
        val cached = cache.get(url)
        if (cached != null) {
            return cached
        }

        val req = Request.Builder().url(url).build()
        val res = okhttp.newCall(req).execute()
        val body = res.body()
        val og = OpenGraphParser.parse(body.charStream().buffered())
        body.close()

        val freshOg = fallback(og, url, req.url().host())
        cache.put(url, freshOg)

        return freshOg
    }

    private fun fallback(og: OpenGraph, url: String, host: String): OpenGraph {
        fun String.getOrElse(default: String): String = if (this.isBlank()) default else this

        val newTitle = og.title.getOrElse(url)
        val newDesc = og.description.getOrElse("")
        val newImage = og.image.getOrElse(FAVICON_BASE_URL + host)
        val newUrl = og.url.getOrElse(url)

        return OpenGraph(newTitle, newDesc, newImage, newUrl)
    }
}
