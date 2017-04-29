package net.yusukezzz.ssmtc.data.og

import nl.komponents.kovenant.task
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.util.*

class OpenGraphClient(private val cache: OGDiskCache, private val okhttp: OkHttpClient) {
    private val tasks: WeakHashMap<OpenGraphLoadable, OpenGraphTask> = WeakHashMap()

    init {
        task { cache.removeOldCaches() }
    }

    fun load(url: String, view: OpenGraphLoadable) {
        synchronized(OpenGraphClient::class) {
            // cancel if there are any old requests for the same view
            tasks.remove(view)?.let(OpenGraphTask::cancel)

            val target = WeakReference<OpenGraphLoadable>(view)
            val t = OpenGraphTask(url, target, okhttp, cache).execute {
                target.get()?.let { v -> tasks.remove(v) }
            }
            tasks.put(view, t)
        }
    }
}
