package net.yusukezzz.ssmtc.data.og

interface OpenGraphLoadable {
    fun onStart()
    fun onComplete(og: OpenGraph)
}

