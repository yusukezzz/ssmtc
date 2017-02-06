package net.yusukezzz.ssmtc.data.og

import android.util.LruCache
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset

class OpenGraphClient {
    companion object {
        private const val MAX_CACHE_SIZE = 20

        private fun createTmpData(url: String): OpenGraph = OpenGraph(url, "", "", url)
    }
    private val cache: LruCache<String, OpenGraph> by lazy { LruCache<String, OpenGraph>(MAX_CACHE_SIZE) }
    private val okhttp = OkHttpClient.Builder().build()
    private lateinit var listener: OpenGraphListener

    interface OpenGraphListener {
        fun onLoaded(pos: Int)
    }

    fun setListener(listener: OpenGraphListener): OpenGraphClient {
        this.listener = listener

        return this
    }

    fun load(url: String, pos: Int): OpenGraph {
        val og = cache.get(url)
        if (og != null) {
            return og
        }

        enqueue(url, pos)

        return createTmpData(url)
    }

    private fun enqueue(url: String, pos: Int) {
        task {
            val req = Request.Builder().url(url).build()
            val res = okhttp.newCall(req).execute()
            val resolvedUrl = res.request().url().toString()
            val body = res.body()
            val headerCharset: Charset? = body.contentType().charset(null)
            val og = if (headerCharset != null) {
                OpenGraphParser.parse(resolvedUrl, body.charStream().buffered())
            } else {
                OpenGraphParser.parse(resolvedUrl, body.bytes())
            }
            body.close()
            og
        } successUi {
            cache.put(url, it)
            listener.onLoaded(pos)
        } failUi ::println
    }
}
