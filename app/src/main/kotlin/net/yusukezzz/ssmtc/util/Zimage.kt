package net.yusukezzz.ssmtc.util

import java.net.URLEncoder

object Zimage {
    private const val MAX_WIDTH = 300
    private const val ENDPOINT_HTTPS = "https://zimage.global.ssl.fastly.net/"

    fun url(imageUrl: String): String = "$ENDPOINT_HTTPS?imageUrl=${encode(imageUrl)}&w=$MAX_WIDTH"

    private fun encode(url: String) = URLEncoder.encode(url, "UTF-8")
}
