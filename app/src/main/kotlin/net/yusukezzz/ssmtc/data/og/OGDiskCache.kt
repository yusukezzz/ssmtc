package net.yusukezzz.ssmtc.data.og

import android.content.Context
import com.jakewharton.disklrucache.DiskLruCache
import net.yusukezzz.ssmtc.util.gson.GsonHolder
import java.io.File

class OGDiskCache(context: Context) {
    companion object {
        private const val CACHE_DIR = "og_cache"
        private const val MAX_CACHE_SIZE = 256L
    }

    private val cache = DiskLruCache.open(File(context.cacheDir, CACHE_DIR), 0, 1, MAX_CACHE_SIZE)

    fun get(url: String): OpenGraph? {
        val data = cache.get(url) ?: return null
        val json = data.getString(0)

        return GsonHolder.gson.fromJson(json, OpenGraph::class.java)
    }

    fun put(url: String, og: OpenGraph) {
        val json = GsonHolder.gson.toJson(og)
        val editor = cache.edit(url)
        editor.set(0, json)
        editor.commit()
    }
}
