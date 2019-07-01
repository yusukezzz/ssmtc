package net.yusukezzz.ssmtc.data.og

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import net.yusukezzz.ssmtc.util.launchUI
import net.yusukezzz.ssmtc.util.withIO
import java.lang.ref.WeakReference

class OpenGraphTask(
    private val mainScope: CoroutineScope,
    private val url: String,
    private val target: WeakReference<OpenGraphLoadable>,
    private val ogApi: OpenGraphApi,
    private val cache: OGDiskCache) {
    companion object {
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "gif", "png")
        private fun ext(url: String): String = url.split(".").last().toLowerCase()
        fun isImageUrl(url: String): Boolean = IMAGE_EXTENSIONS.contains(ext(url))
    }

    private class OGHttpErrorException(message: String) : Exception(message)
    private class OGCancelException : CancellationException()

    private var job: Job? = null

    fun execute(done: () -> Unit): OpenGraphTask {
        job = mainScope.launchUI {
            var og: OpenGraph? = null
            try {
                og = withIO { resolve() }
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

    private suspend fun resolve(): OpenGraph {
        val cached = cache.get(url)
        if (cached != null) {
            return cached
        }

        if (isImageUrl(url)) {
            return OpenGraph.imageData(url)
        }

        val res = ogApi.parse(url)
        if (!res.isSuccessful) {
            throw OGHttpErrorException("OpenGraph HTTP error: $url")
        }
        val og = res.body()!!
        cache.put(url, og)

        return og
    }

    fun cancel() {
        job?.cancelChildren(OGCancelException())
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
