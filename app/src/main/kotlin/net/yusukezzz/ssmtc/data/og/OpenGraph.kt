package net.yusukezzz.ssmtc.data.og

data class OpenGraph(val title: String, val image: String, val url: String) {
    companion object {
        fun tmpData(url: String): OpenGraph = OpenGraph(url, "", url)
        fun imageData(url: String): OpenGraph = OpenGraph(url, url, url)
    }
}
