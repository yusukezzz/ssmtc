package net.yusukezzz.ssmtc.util

object Rszio {
    fun url(imageUrl: String, width: Int = 200): String {
        val url = imageUrl.replaceFirst(Regex("://"), "://rsz.io/")
        return if (url.contains("?")) {
            "$url&w=$width"
        } else {
            "$url?w=$width"
        }
    }
}
