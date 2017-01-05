package net.yusukezzz.ssmtc.util.picasso

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object PicassoUtil {
    const val MAX_CACHE_SIZE: Long = 100L * 1024 * 1024 // 100MB
    const val CACHE_DIR: String = "picasso-cache"

    fun clean(view: View): Unit {
        if (view is ImageView) {
            Picasso.with(view.context).cancelRequest(view)
            view.setImageDrawable(null)
        }
    }

    private fun cacheDir(context: Context): File = File(context.applicationContext.cacheDir, CACHE_DIR)
    private fun buildDownloader(cache: Cache): Downloader {
        val okhttp = OkHttpClient.Builder()
            .cache(cache)
            .build()

        return OkHttp3Downloader(okhttp)
    }

    fun downloader(context: Context): Downloader {
        val cache = Cache(cacheDir(context), MAX_CACHE_SIZE)
        return buildDownloader(cache)
    }
}
