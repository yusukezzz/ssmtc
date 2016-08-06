package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.json.Media
import net.yusukezzz.ssmtc.data.json.Url

open class FormattedUrl(urlEntity: Url) {
    val start: Int = urlEntity.start
    val end: Int = urlEntity.end
    val shortUrl: String = urlEntity.url
    val displayUrl: String = urlEntity.display_url
    val url: String = urlEntity.expanded_url
}

class FormattedMedia(mediaEntity: Media): FormattedUrl(mediaEntity.urlEntity) {
    val type: String = mediaEntity.type
}
