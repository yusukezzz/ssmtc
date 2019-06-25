package net.yusukezzz.ssmtc.data.og

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.ref.WeakReference
import java.util.*
import kotlin.coroutines.CoroutineContext

class OpenGraphService(private val cache: OGDiskCache, private val ogApi: OpenGraphApi) {
    private val tasks: WeakHashMap<OpenGraphLoadable, OpenGraphTask> = WeakHashMap()

    suspend fun load(coroutineContext: CoroutineContext, url: String, view: OpenGraphLoadable) {
        view.onStart()
        // cancel if there are any old requests for the same view
        tasks.remove(view)?

        val target = WeakReference<OpenGraphLoadable>(view)
        val t = OpenGraphTask(url, target, ogApi, cache).execute {
            target.get()?.let(tasks::remove)
        }
        tasks[view] = t
    }

    suspend fun cleanup() = cache.removeOldCaches()
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
