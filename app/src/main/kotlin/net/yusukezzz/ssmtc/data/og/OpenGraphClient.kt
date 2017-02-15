package net.yusukezzz.ssmtc.data.og

import android.content.Context
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.lang.ref.WeakReference

class OpenGraphClient(context: Context) {
    companion object {
        private fun createTmpData(url: String): OpenGraph = OpenGraph(url, "", url)
        private fun createImageData(url: String): OpenGraph = OpenGraph(url, url, url)
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "gif", "png")
    }

    private val cache = OGDiskCache(context)
    private val okhttp = OkHttpClient.Builder().build()

    interface OpenGraphLoadable {
        fun onLoad(og: OpenGraph)
    }

    fun load(url: String, view: OpenGraphLoadable) {
        if (isImageUrl(url)) {
            view.onLoad(createImageData(url))
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

    private fun enqueue(url: String, view: OpenGraphLoadable) {
        val target = WeakReference<OpenGraphLoadable>(view)
        // FIXME: Make it possible to cancel when the view is recycled before the async task is completed
        task {
            resolve(url)
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

    private fun resolve(url: String): OpenGraph {
        // request redirected url and content-type without body
        val headReq = Request.Builder().url(url).head().build()
        val headRes = okhttp.newCall(headReq).execute()
        val resolvedUrl = headRes.request().url().toString()

        val cached = cache.get(resolvedUrl)
        if (cached != null) {
            return cached
        }

        val headBody = headRes.body()
        val contentType = headBody.contentType()
        headBody.close()
        // ignore non HTML content
        if (headRes.isSuccessful && contentType.isNotHtml()) {
            val tmp = createTmpData(resolvedUrl)
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
