package net.yusukezzz.ssmtc.data.og

import nl.komponents.kovenant.Kovenant
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import okhttp3.*
import java.lang.ref.WeakReference

class OpenGraphTask(val url: String,
                    val target: WeakReference<OpenGraphLoadable>,
                    private val okhttp: OkHttpClient,
                    private val cache: OGDiskCache) {
    companion object {
        const val MAX_CONTENT_SIZE = 64 * 1024 // 64KB

        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "gif", "png")
        private fun ext(url: String): String = url.split(".").last().toLowerCase()
        fun isImageUrl(url: String): Boolean = IMAGE_EXTENSIONS.contains(ext(url))
    }

    private var call: Call? = null
    private var realTask: Promise<OpenGraph, Exception>? = null

    fun execute(done: () -> Unit): OpenGraphTask {
        realTask = task {
            resolve()
        } successUi {
            target.get()?.onLoad(it)
        } failUi {
            it.printStackTrace()
            if (it !is OGCancelException) {
                target.get()?.onLoad(fallback(url))
            }
        } always {
            done()
        }

        return this
    }

    private fun resolve(): OpenGraph {
        // request redirected url and content-type without body
        call = okhttp.newCall(Request.Builder().url(url).head().build())
        val headRes = call!!.execute()
        val resolvedUrl = headRes.request().url().toString()
        val headBody = headRes.body()
        val contentType = headBody.contentType()
        headBody.close()
        val contentSize = headBody.contentLength()

        val cached = cache.get(resolvedUrl)
        if (cached != null) {
            return cached
        }

        // ignore non HTML content
        if (headRes.isSuccessful && contentType.isNotHtml()) {
            return fallback(resolvedUrl)
        }

        // ignore large HTML document
        // TODO: through if WIFI connected?
        if (MAX_CONTENT_SIZE <= contentSize) {
            return fallback(resolvedUrl)
        }

        // request HTML body
        call = okhttp.newCall(Request.Builder().url(resolvedUrl).build())
        val res = call!!.execute()
        val body = res.body()
        val og = parseHtml(resolvedUrl, body)
        cache.put(url, og)

        return og
    }

    private class OGCancelException : Exception()

    fun cancel() {
        call?.let(Call::cancel)
        realTask?.let { Kovenant.cancel(it, OGCancelException()) }
    }

    private fun fallback(resolvedUrl: String): OpenGraph {
        val tmp = if (isImageUrl(resolvedUrl)) {
            OpenGraph.imageData(resolvedUrl)
        } else {
            OpenGraph.tmpData(resolvedUrl)
        }
        cache.put(resolvedUrl, tmp)
        return tmp
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
