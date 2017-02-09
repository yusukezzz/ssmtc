package net.yusukezzz.ssmtc.data.og

import android.content.Context
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.alwaysUi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset

class OpenGraphClient(context: Context) {
    companion object {
        private fun createTmpData(url: String): OpenGraph = OpenGraph(url, "", url)
    }

    private val cache = OGDiskCache(context)
    private val okhttp = OkHttpClient.Builder().build()
    private lateinit var listener: OpenGraphListener

    interface OpenGraphListener {
        fun onLoaded(pos: Int)
    }

    fun setListener(listener: OpenGraphListener): OpenGraphClient {
        this.listener = listener

        return this
    }

    fun load(url: String, pos: Int): OpenGraph? {
        val og: OpenGraph? = cache.get(url)
        if (og != null) {
            return og
        }

        enqueue(url, pos)

        return null
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
            cache.put(url, og)
        } fail {
            println(it)
            it.printStackTrace()
            val og = createTmpData(url)
            cache.put(url, og)
        } alwaysUi {
            listener.onLoaded(pos)
        }
    }
}
