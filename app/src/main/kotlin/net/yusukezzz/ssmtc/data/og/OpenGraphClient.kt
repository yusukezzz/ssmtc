package net.yusukezzz.ssmtc.data.og

import nl.komponents.kovenant.Kovenant
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.lang.ref.WeakReference
import java.util.*

class OpenGraphClient(private val cache: OGDiskCache, private val okhttp: OkHttpClient) {
    companion object {
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "gif", "png")
    }

    private val tasks: WeakHashMap<OpenGraphLoadable, Promise<OpenGraph, Exception>> = WeakHashMap()

    interface OpenGraphLoadable {
        fun onLoad(og: OpenGraph)
    }

    init {
        task { cache.removeOldCaches() }
    }

    fun load(url: String, view: OpenGraphLoadable) {
        if (isImageUrl(url)) {
            view.onLoad(OpenGraph.imageData(url))
            return
        }

        val og: OpenGraph? = cache.get(url)
        if (og != null) {
            view.onLoad(og)
        } else {
            enqueue(url, view)
        }
    }

    private fun ext(url: String): String = url.split(".").last().toLowerCase()

    private fun isImageUrl(url: String): Boolean = IMAGE_EXTENSIONS.contains(ext(url))

    private class OGCancelException : Exception()
    private fun enqueue(url: String, view: OpenGraphLoadable) {
        synchronized(view) {
            val target = WeakReference<OpenGraphLoadable>(view)
            // cancel if there are any old requests for the same view
            tasks.remove(view)?.let {
                Kovenant.cancel(it, OGCancelException())
            }
            val t = task {
                resolve(url)
            } successUi {
                target.get()?.onLoad(it)
            } failUi {
                println(it)
                it.printStackTrace()
                if (it !is OGCancelException) {
                    val og = OpenGraph.tmpData(url)
                    cache.put(url, og)
                    target.get()?.onLoad(og)
                }
            } always {
                target.get()?.let { tasks.remove(it) }
            }
            tasks.put(view, t)
        }
    }

    private fun resolve(url: String): OpenGraph {
        // request redirected url and content-type without body
        val headReq = Request.Builder().url(url).head().build()
        val headRes = okhttp.newCall(headReq).execute()
        val resolvedUrl = headRes.request().url().toString()
        val headBody = headRes.body()
        val contentType = headBody.contentType()
        headBody.close()

        val cached = cache.get(resolvedUrl)
        if (cached != null) {
            return cached
        }

        // ignore non HTML content
        if (headRes.isSuccessful && contentType.isNotHtml()) {
            val tmp = if (isImageUrl(resolvedUrl)) {
                OpenGraph.imageData(resolvedUrl)
            } else {
                OpenGraph.tmpData(resolvedUrl)
            }
            cache.put(resolvedUrl, tmp)
            return tmp
        }

        // request HTML
        val req = Request.Builder().url(resolvedUrl).build()
        val res = okhttp.newCall(req).execute()
        val body = res.body()
        val og = parseHtml(resolvedUrl, body)
        cache.put(url, og)

        return og
    }

    private fun parseHtml(resolvedUrl: String, body: ResponseBody): OpenGraph = body.use {
        try {
            if (body.contentType().hasCharset()) {
                OpenGraphParser.parse(resolvedUrl, body.charStream().buffered())
            } else {
                OpenGraphParser.parse(resolvedUrl, body.bytes())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun MediaType.isNotHtml(): Boolean = this.subtype() != "html"
    private fun MediaType.hasCharset(): Boolean = this.charset(null) != null
}
