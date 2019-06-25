package net.yusukezzz.ssmtc.data.og

interface OpenGraphLoadable {
    suspend fun onStart()
    suspend fun onComplete(og: OpenGraph)
}

