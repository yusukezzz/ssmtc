package net.yusukezzz.ssmtc.data.og

import com.google.gson.Gson
import org.apache.commons.codec.digest.DigestUtils
import org.threeten.bp.Instant
import java.io.File

class OGDiskCache(private val appCacheDir: File,
                  private val gson: Gson) {
    companion object {
        private const val CACHE_DIR_NAME = "og_cache"
        private const val CACHE_EXPIRE_MILLI_SECONDS = 86400 * 3 * 1000L // 3 days
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
    }

    fun removeOldCaches() = synchronized(appCacheDir) {
        val expired = Instant.now().toEpochMilli() - CACHE_EXPIRE_MILLI_SECONDS
        cacheDir.listFiles().toList().forEach {
            if (it.lastModified() < expired) {
                it.delete()
            }
        }
    }

    private fun prepareCacheFile(url: String): File = File(cacheDir.apply { mkdirs() }, DigestUtils.md5Hex(url) + ".json")
}
