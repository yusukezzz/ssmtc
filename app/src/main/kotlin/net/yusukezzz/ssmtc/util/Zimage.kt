package net.yusukezzz.ssmtc.util

import java.net.URLEncoder

object Zimage {
    private const val ENDPOINT_HTTPS = "http://edge.zimage.io/"

    fun url(imageUrl: String, width: Int = 200): String = "$ENDPOINT_HTTPS?url=${encode(imageUrl)}&w=$width"

    private fun encode(url: String) = URLEncoder.encode(url, "UTF-8")
}
