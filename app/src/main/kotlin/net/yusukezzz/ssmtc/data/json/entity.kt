package net.yusukezzz.ssmtc.data.json

import nz.bradcampbell.paperparcel.PaperParcel

@PaperParcel
data class Entity(
    val urls: List<Url>?,
    val media: List<Media>?
)

@PaperParcel
data class Url(
    val url: String,
    val expanded_url: String,
    val display_url: String,
    val indices: List<Int>
) {
    val start: Int
        get() = indices[0]
    val end: Int
        get() = indices[1]
}

@PaperParcel
data class Media(
    val id: Long,
    val id_str: String,
    val type: String,
    val media_url: String,
    val media_url_https: String,
    val url: String,
    val expanded_url: String,
    val display_url: String,
    val indices: List<Int>,
    val video_info: VideoInfo?
) {
    companion object {
        val TYPE_PHOTO = "photo"
        val TYPE_GIF = "gif"
        val TYPE_VIDEO = "video"
    }

    val isPhoto: Boolean
        get() = type == TYPE_PHOTO

    val isVideo: Boolean
        get() = type == TYPE_GIF || type == TYPE_VIDEO

    val urlEntity: Url
        get() = Url(url, expanded_url, display_url, indices)

    val thumb_url: String
        get() = media_url + ":thumb"
    val small_url: String
        get() = media_url + ":small"
    val medium_url: String
        get() = media_url + ":medium"
    val large_url: String
        get() = media_url + ":large"
}

@PaperParcel
data class VideoInfo(
    val aspect_ratio: List<Int>,
    val duration_millis: Int,
    val variants: List<VideoVariant>
) {
    companion object {
        val TYPE_MP4 = "video/mp4"
    }

    val allMp4: List<VideoVariant>
        get() = variants.filter { it.content_type == TYPE_MP4 }.sortedByDescending { it.bitrate }
    val mp4High: VideoVariant
        get() = allMp4.first()
    val mp4Mid: VideoVariant
        get() = allMp4.getOrElse(1, { mp4High })
    val mp4Small: VideoVariant
        get() = allMp4.getOrElse(2, { mp4Mid })
}

@PaperParcel
data class VideoVariant(
    val bitrate: Int,
    val content_type: String,
    val url: String
)

data class UploadResult(
    val media_id: Long,
    val media_id_string: String,
    val size: Long,
    val image: UploadImage
)

data class UploadImage(
    val w: Int,
    val h: Int,
    val image_type: String
)

