package net.yusukezzz.ssmtc.data.og

import java.io.BufferedReader

object OpenGraphParser {
    val OG_TITLE = Regex("(og|twitter):title")
    val OG_DESC = Regex("(og|twitter):description")
    val OG_IMAGE = Regex("\"(og|twitter):image\"")
    val OG_URL = Regex("(og|twitter):url")

    val META_OPTS = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    val META_TAG = Regex("<meta.+?>", META_OPTS)
    val HEAD_END_TAG = Regex("</head>", META_OPTS)
    const val CONTENT_KEY = "content=\""

    fun parse(bufferedReader: BufferedReader): OpenGraph = parseMeta(parseHead(bufferedReader))

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

    private fun parseMeta(head: CharSequence): OpenGraph {
        var title = ""
        var desc = ""
        var image = ""
        var url = ""

        META_TAG.findAll(head).forEach {
            val meta = it.value
            if (meta.contains(OG_TITLE)) {
                title = extractContent(meta)
            } else if (meta.contains(OG_DESC)) {
                desc = extractContent(meta)
            } else if (meta.contains(OG_IMAGE)) {
                image = extractContent(meta)
            } else if (meta.contains(OG_URL)) {
                url = extractContent(meta)
            }
        }

        return OpenGraph(title, desc, image, url)
    }

    private fun extractContent(meta: String): String {
        val start = meta.indexOf(CONTENT_KEY) + CONTENT_KEY.length
        val end = meta.indexOf("\"", start)

        return meta.substring(start, end).replace(Regex("(\r\n|\r|\n)"), "").trim()
    }
}
