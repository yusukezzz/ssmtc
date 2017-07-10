package net.yusukezzz.ssmtc.data.og

import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object OpenGraphParser {
    private val OG_TITLE = Regex("og:title")
    private val OG_IMAGE = Regex("(\"og:image\"|'og:image')")
    private val OG_URL = Regex("og:url")

    private val REGEX_OPTS = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    private val META_TAG = Regex("<meta.+?>", REGEX_OPTS)
    private val META_CHARSET_TAG = Regex("<meta.+?charset=(.+?)(\\s|/?>)", REGEX_OPTS)
    private val TITLE_TAG = Regex("<title.*?>(.+?)</title>", REGEX_OPTS)
    private val HEAD_END_TAG = Regex("</head>", REGEX_OPTS)
    private const val CONTENT_KEY = "content="

    // parse from decoded HTML
    fun parse(url: String, bufferedReader: BufferedReader): OpenGraph = parseMeta(url, parseHead(bufferedReader))

    // guess charset from raw string HTML
    fun parse(url: String, bytes: InputStream): OpenGraph {
        val headBytes = parseHeadBytes(bytes)
        val charset = detectCharset(headBytes.toString(StandardCharsets.US_ASCII))
        return parseMeta(url, headBytes.toString(charset))
    }

    private fun parseHead(bufferedReader: BufferedReader): CharSequence {
        val head = StringBuilder()
        bufferedReader.use {
            var line: String? = it.readLine()
            while (line != null) {
                head.append(line)
                if (line.contains(HEAD_END_TAG)) {
                    break
                }
                line = it.readLine()
            }
        }

        return head
    }

    private fun parseHeadBytes(bytes: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        var prev = ByteArray(0)
        bytes.use {
            while (true) {
                val len = it.read(buf)
                if (len < 0) {
                    break
                }
                out.write(buf, 0, len)

                val tmp = (prev + buf).toString(StandardCharsets.US_ASCII)
                if (tmp.contains(HEAD_END_TAG)) {
                    break
                }
                prev = buf
            }
        }

        return out.use { it.toByteArray() }
    }

    private fun detectCharset(head: CharSequence): Charset {
        val matched = META_CHARSET_TAG.find(head)
        return if (matched != null) {
            val charsetName = matched.groupValues[1].replace("\"", "").replace("'", "")
            Charset.forName(charsetName)
        } else {
            StandardCharsets.UTF_8
        }
    }

    private fun parseMeta(url: String, head: CharSequence): OpenGraph {
        var title = ""
        var image = ""
        var ogUrl = ""

        META_TAG.findAll(head).forEach {
            val meta = it.value
            if (title.isEmpty() && meta.contains(OG_TITLE)) {
                title = StringEscapeUtils.unescapeHtml4(extractContent(meta))
            } else if (image.isEmpty() && meta.contains(OG_IMAGE)) {
                image = extractContent(meta)
            } else if (ogUrl.isEmpty() && meta.contains(OG_URL)) {
                ogUrl = extractContent(meta)
            }
        }

        if (title.isEmpty()) {
            // use html title if available
            TITLE_TAG.find(head)?.let { title = StringEscapeUtils.unescapeHtml4(it.groupValues[1].trim()) }
        }

        // fallback
        if (title.isEmpty()) title = url
        if (!ogUrl.startsWith("http")) ogUrl = url

        return OpenGraph(title, image, ogUrl)
    }

    private fun extractContent(originalMeta: String): String {
        val meta = originalMeta.replace(Regex("content\\s*?=\\s*?", REGEX_OPTS), CONTENT_KEY)
        val quoteAt = meta.indexOf(CONTENT_KEY) + CONTENT_KEY.length
        val quote = meta[quoteAt] // single or double quote char
        val start = quoteAt + 1
        val end = meta.indexOf(quote, start)

        return meta.substring(start, end).replace(Regex("(\r\n|\r|\n)"), "").trim()
    }
}
