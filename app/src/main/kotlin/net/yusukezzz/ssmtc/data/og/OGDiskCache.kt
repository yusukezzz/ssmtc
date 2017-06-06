package net.yusukezzz.ssmtc.data.og

import com.google.gson.Gson
import org.apache.commons.codec.digest.DigestUtils
import org.threeten.bp.Instant
import java.io.File

class OGDiskCache(appCacheDir: File,
                  private val gson: Gson) {
    companion object {
        private const val CACHE_DIR_NAME = "og_cache"
        private const val CACHE_EXPIRE_MILLI_SECONDS = 86400 * 1000L // 1 day
    }
    private val cacheDir = File(appCacheDir, CACHE_DIR_NAME)

    init {
        cacheDir.mkdirs()
    }

    fun get(url: String): OpenGraph? = synchronized(OGDiskCache::class) {
        val cache = prepareCacheFile(url)
        if (!cache.exists()) {
            return null
        }
        return gson.fromJson(cache.readText(), OpenGraph::class.java)
        //.let { it.copy(title = "[C] " + it.title) } // append cache mark for debug
    }

    fun put(url: String, og: OpenGraph) = synchronized(OGDiskCache::class) {
        prepareCacheFile(url).writeText(gson.toJson(og))
    }

    fun removeOldCaches() = synchronized(OGDiskCache::class) {
        val expired = Instant.now().toEpochMilli() - CACHE_EXPIRE_MILLI_SECONDS
        cacheDir.listFiles().toList().forEach {
            if (it.lastModified() < expired) {
                it.delete()
            }
        }
    }

    private fun prepareCacheFile(url: String): File = File(cacheDir, DigestUtils.md5Hex(url) + ".json")
}
