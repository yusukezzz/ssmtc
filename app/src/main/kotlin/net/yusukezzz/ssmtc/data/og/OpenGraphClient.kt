package net.yusukezzz.ssmtc.data.og

import android.util.LruCache
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.successUi
import okhttp3.OkHttpClient
import okhttp3.Request

class OpenGraphClient {
    companion object {
        private const val MAX_CACHE_SIZE = 20
    }
    private val cache: LruCache<String, OpenGraph> by lazy { LruCache<String, OpenGraph>(MAX_CACHE_SIZE) }
    private val okhttp = OkHttpClient.Builder().build()
    private lateinit var observer: OpenGraphObserver

    interface OpenGraphObserver {
        fun onLoaded()
    }

    fun setObserver(observer: OpenGraphObserver) {
        this.observer = observer
    }

    fun load(url: String): OpenGraph {
        val og = cache.get(url)
        if (og != null) {
            return og
        }

        enqueue(url)

        // pass temporary data
        return OpenGraph(url, "", "", url)
    }

    private fun enqueue(url: String) {
        task {
            val req = Request.Builder().url(url).build()
            val res = okhttp.newCall(req).execute()
            val body = res.body()
            val og = OpenGraphParser.parse(body.charStream().buffered())
            body.close()
            og
        } successUi {
            cache.put(url, it)
            observer.onLoaded()
        }
    }
}
