package net.yusukezzz.ssmtc.data.og

import kotlinx.coroutines.CoroutineScope
import net.yusukezzz.ssmtc.util.launchUI
import net.yusukezzz.ssmtc.util.withIO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.ref.WeakReference
import java.util.*

class OpenGraphService(
    private val cache: OGDiskCache,
    private val ogApi: OpenGraphApi,
    private val mainScope: CoroutineScope
) {
    private val tasks: MutableMap<OpenGraphLoadable, OpenGraphTask> = Collections.synchronizedMap(WeakHashMap())

    fun load(url: String, view: OpenGraphLoadable) {
        view.onStart()
        // cancel if there are any old requests for the same view
        tasks.remove(view)?.cancel()

        val target = WeakReference(view)
        val t = OpenGraphTask(mainScope, url, target, ogApi, cache).execute {
            target.get()?.let(tasks::remove)
        }
        tasks[view] = t
    }

    fun cleanup() = mainScope.launchUI {
        withIO { cache.removeOldCaches() }
    }
}

interface OpenGraphApi {
    /**
     * {
     *      url: "http://example.com/...",
     *      title: "The opengraph title",
     *      image: "The opengraph image url"
     * }
     */
    @GET("/opengraph/{url}")
    suspend fun parse(@Path("url") url: String): Response<OpenGraph>
}
