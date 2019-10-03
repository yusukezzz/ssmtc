package net.yusukezzz.ssmtc.ui.status.update

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.InputStream

class InputStreamBody(
    private val input: InputStream,
    private val length: Long,
    private val mediaType: MediaType?
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink): Unit = input.source().use { sink.writeAll(it) }
}
