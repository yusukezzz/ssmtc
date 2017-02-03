package net.yusukezzz.ssmtc.data.og

data class OpenGraph(val title: String,
                     val description: String,
                     val image: String,
                     val url: String) {

    val isValid: Boolean
        get() = (title.isNotEmpty() && url.isNotEmpty())
}
