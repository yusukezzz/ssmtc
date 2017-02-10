package net.yusukezzz.ssmtc.data.og

import android.content.Context
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.lang.ref.WeakReference

class OpenGraphClient(context: Context) {
    companion object {
        private fun createTmpData(url: String): OpenGraph = OpenGraph(url, "", url)
    }

    private val cache = OGDiskCache(context)
    private val okhttp = OkHttpClient.Builder().build()

    interface OpenGraphLoadable {
        fun onLoad(og: OpenGraph)
    }

    fun load(url: String, view: OpenGraphLoadable) {
        val og: OpenGraph? = cache.get(url)
        if (og != null) {
            view.onLoad(og)
        } else {
            enqueue(url, view)
        }
    }

    private fun enqueue(url: String, view: OpenGraphLoadable) {
        val target = WeakReference<OpenGraphLoadable>(view)
        // FIXME: Make it possible to cancel when the view is recycled before the async task is completed
        task {
            val req = Request.Builder().url(url).build()
            val res = okhttp.newCall(req).execute()
            val resolvedUrl = res.request().url().toString()
            val body = res.body()
            val og = if (hasCharset(body)) {
                OpenGraphParser.parse(resolvedUrl, body.charStream().buffered())
            } else {
                OpenGraphParser.parse(resolvedUrl, body.bytes())
            }
            body.close()
            cache.put(url, og)
            og
        } successUi {
            target.get()?.onLoad(it)
        } failUi {
            println(it)
            it.printStackTrace()
            val og = createTmpData(url)
            cache.put(url, og)
            target.get()?.onLoad(og)
        }
    }

    private fun hasCharset(body: ResponseBody): Boolean = body.contentType().charset(null) != null
}
