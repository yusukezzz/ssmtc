package net.yusukezzz.ssmtc.data.og

import com.google.gson.Gson
import org.apache.commons.codec.digest.DigestUtils
import java.io.File

class OGDiskCache(private val appCacheDir: File,
                  private val gson: Gson) {
    companion object {
        private const val CACHE_DIR_NAME = "og_cache"
        private const val MAX_CACHE_FILES = 1000
    }

    private val cacheDir = File(appCacheDir, CACHE_DIR_NAME)

    fun get(url: String): OpenGraph? = synchronized(appCacheDir) {
        val cache = prepareCacheFile(url)
        if (!cache.exists()) {
            return null
        }
        return gson.fromJson(cache.readText(), OpenGraph::class.java)
    }

    fun put(url: String, og: OpenGraph) = synchronized(appCacheDir) {
        prepareCacheFile(url).writeText(gson.toJson(og))
        removeOldCaches()
    }

    private fun removeOldCaches() {
        val caches = cacheDir.listFiles().toList()
        val deletions = caches.size - MAX_CACHE_FILES
        if (deletions > 0) {
            caches.sortedBy(File::lastModified).slice(0..(deletions - 1)).forEach { it.delete() }
        }
    }

    private fun prepareCacheFile(url: String): File = File(cacheDir.apply { mkdirs() }, DigestUtils.md5Hex(url) + ".json")
}
