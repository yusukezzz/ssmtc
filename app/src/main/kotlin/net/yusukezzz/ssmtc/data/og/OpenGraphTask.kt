package net.yusukezzz.ssmtc.data.og

import kotlinx.coroutines.experimental.Job
import net.yusukezzz.ssmtc.util.async
import net.yusukezzz.ssmtc.util.ui
import retrofit2.Call
import java.lang.ref.WeakReference

class OpenGraphTask(private val url: String,
                    private val target: WeakReference<OpenGraphLoadable>,
                    private val ogApi: OpenGraphApi,
                    private val cache: OGDiskCache) {
    companion object {
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "gif", "png")
        private fun ext(url: String): String = url.split(".").last().toLowerCase()
        fun isImageUrl(url: String): Boolean = IMAGE_EXTENSIONS.contains(ext(url))
    }

    private class OGHttpErrorException(message: String) : Exception(message)
    private class OGCancelException : Exception()

    private var httpCall: Call<OpenGraph>? = null
    private var realTask: Job? = null

    fun execute(done: () -> Unit): OpenGraphTask {
        realTask = ui {
            try {
                val og = async { resolve() }.await()
                target.get()?.onComplete(og)
            } catch (e: Exception) {
                println("OpenGraphTask failed: $e")
                if (e !is OGCancelException) {
                    println(url)
                    e.printStackTrace()
                    target.get()?.onComplete(fallback(url))
                }
            } finally {
                done()
            }
        }

        return this
    }

    private fun resolve(): OpenGraph {
        val cached = cache.get(url)
        if (cached != null) {
            return cached
        }

        if (isImageUrl(url)) {
            return OpenGraph.imageData(url)
        }

        httpCall = ogApi.parse(url)
        val res = httpCall!!.execute()
        if (!res.isSuccessful) {
            throw OGHttpErrorException("OpenGraph HTTP error: $url")
        }
        val og = res.body()!!
        cache.put(url, og)

        return og
    }

    fun cancel() {
        httpCall?.cancel()
        realTask?.cancel(OGCancelException())
    }

    private fun fallback(resolvedUrl: String): OpenGraph {
        val tmp = if (isImageUrl(resolvedUrl)) {
            OpenGraph.imageData(resolvedUrl)
        } else {
            OpenGraph.tmpData(resolvedUrl)
        }
        cache.put(url, tmp)
        return tmp
    }
}
