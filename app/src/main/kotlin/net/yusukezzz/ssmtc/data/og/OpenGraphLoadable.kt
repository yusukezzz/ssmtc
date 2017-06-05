package net.yusukezzz.ssmtc.data.og

interface OpenGraphLoadable {
    fun onStart()
    fun onLoading()
    fun onComplete(og: OpenGraph)
}

