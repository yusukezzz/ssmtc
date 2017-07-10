package net.yusukezzz.ssmtc.data.og

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OpenGraphParserTest {
    @Test
    fun parseEncoded() {
        val reader = this.javaClass.classLoader.getResourceAsStream("html/utf8.html").bufferedReader(Charsets.UTF_8)
        val og = OpenGraphParser.parse("test", reader)

        assertEquals("OGタイトル UTF-8", og.title)
        assertEquals("http://example.com", og.url)
        assertEquals("http://image.com", og.image)
    }

    @Test
    fun parseBytes() {
        val input = this.javaClass.classLoader.getResourceAsStream("html/sjis.html")
        val og = OpenGraphParser.parse("test", input)

        assertEquals("OGタイトル SJIS", og.title)
        assertEquals("http://example.com", og.url)
        assertEquals("http://image.com", og.image)
    }
}
