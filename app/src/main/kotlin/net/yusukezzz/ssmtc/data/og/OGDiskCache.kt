package net.yusukezzz.ssmtc.data.og

import android.content.Context
import net.yusukezzz.ssmtc.util.gson.GsonHolder
import org.apache.commons.codec.digest.DigestUtils
import java.io.File

class OGDiskCache(val context: Context) {
    companion object {
        private const val CACHE_DIR = "og_cache"
        private const val MAX_CACHE_FILES = 1000
    }

    private val cacheDir = File(context.cacheDir, CACHE_DIR)

    init {
        cacheDir.mkdirs()
    }

    fun get(url: String): OpenGraph? = synchronized(context) {
        val cache = cacheFile(url)
        if (!cache.exists()) {
            return null
        }
        return GsonHolder.gson.fromJson(cache.readText(), OpenGraph::class.java)
    }

    fun put(url: String, og: OpenGraph) = synchronized(context) {
        cacheFile(url).writeText(GsonHolder.gson.toJson(og))
        removeOldCaches()
    }

    private fun removeOldCaches() {
        val caches = cacheDir.listFiles().toList()
        val deletions = caches.size - MAX_CACHE_FILES
        if (deletions > 0) {
            caches.sortedBy(File::lastModified).slice(0..(deletions - 1)).forEach { it.delete() }
        }
    }

    private fun cacheFile(url: String): File = File(cacheDir, DigestUtils.md5Hex(url) + ".json")
}
