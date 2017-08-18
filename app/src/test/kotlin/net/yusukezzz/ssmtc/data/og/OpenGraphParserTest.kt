package net.yusukezzz.ssmtc.data.og

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset

@RunWith(JUnit4::class)
class OpenGraphParserTest {
    @Test
    fun parseEncoded() {
        val reader = loadEncodedResource("html/utf8.html")
        val og = OpenGraphParser.parse("url", reader)

        assertEquals("OGタイトル UTF-8", og.title)
        assertEquals("http://example.com", og.url)
        assertEquals("http://image.com", og.image)
    }

    @Test
    fun parseEncodedTwitter() {
        val reader = loadEncodedResource("html/utf8_twitter.html")
        val og = OpenGraphParser.parse("url", reader)

        assertEquals("OGタイトル UTF-8", og.title)
        assertEquals("url", og.url)
        assertEquals("http://image.com", og.image)
    }

    @Test
    fun parseBytes() {
        val input = loadResource("html/sjis.html")
        val og = OpenGraphParser.parse("url", input)

        assertEquals("OGタイトル SJIS", og.title)
        assertEquals("http://example.com", og.url)
        assertEquals("http://image.com", og.image)
    }

    private fun loadEncodedResource(file: String, charset: Charset = Charsets.UTF_8): BufferedReader = loadResource(file).bufferedReader(charset)
    private fun loadResource(file: String): InputStream = this.javaClass.classLoader.getResourceAsStream(file)
}
