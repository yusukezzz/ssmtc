package net.yusukezzz.ssmtc.data.og

import net.yusukezzz.ssmtc.util.async
import net.yusukezzz.ssmtc.util.ui
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.ref.WeakReference
import java.util.*

class OpenGraphService(private val cache: OGDiskCache, private val ogApi: OpenGraphApi) {
    private val tasks: WeakHashMap<OpenGraphLoadable, OpenGraphTask> = WeakHashMap()

    fun load(url: String, view: OpenGraphLoadable) {
        synchronized(OpenGraphService::class) {
            view.onStart()
            // cancel if there are any old requests for the same view
            tasks.remove(view)?.cancel()

            val target = WeakReference<OpenGraphLoadable>(view)
            val t = OpenGraphTask(url, target, ogApi, cache).execute {
                target.get()?.let(tasks::remove)
            }
            tasks.put(view, t)
        }
    }

    fun cleanup() = ui {
        async { cache.removeOldCaches() }.await()
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
    fun parse(@Path("url") url: String): Call<OpenGraph>
}
