package net.yusukezzz.ssmtc.data.og

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
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
    private var realTask: Deferred<OpenGraph>? = null

    fun execute(done: () -> Unit): OpenGraphTask {
        ui {
            var og: OpenGraph? = null
            try {
                realTask = async { resolve() }
                og = realTask?.await()
            } catch (e: CancellationException) {
                println("async canceled: $e")
            } catch (e: Exception) {
                println("OpenGraphTask failed: $e")
            } finally {
                if (og == null) og = fallback(url)
                target.get()?.onComplete(og)
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
