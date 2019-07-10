package net.yusukezzz.ssmtc.data.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Entity(
    val urls: List<Url> = listOf(),
    val media: List<Media> = listOf()
) : Parcelable

@Parcelize
data class Url(
    val url: String,
    val expanded_url: String,
    val display_url: String,
    val indices: List<Int>
) : Parcelable {
    val start: Int
        get() = indices[0]
    val end: Int
        get() = indices[1]
}

@Parcelize
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
) : Parcelable {
    companion object {
        const val TYPE_PHOTO = "photo"
        const val TYPE_ANIMATED_GIF = "animated_gif"
        const val TYPE_VIDEO = "video"
    }

    val isPhoto: Boolean
        get() = (type == TYPE_PHOTO)

    val isVideo: Boolean
        get() = (type == TYPE_ANIMATED_GIF || type == TYPE_VIDEO)

    val isGif: Boolean
        get() = (type == TYPE_ANIMATED_GIF)

    val urlEntity: Url
        get() = Url(url, expanded_url, display_url, indices)

    val thumbUrl: String
        get() = "$media_url:thumb"
    val smallUrl: String
        get() = "$media_url:small"
    val mediumUrl: String
        get() = "$media_url:medium"
    val largeUrl: String
        get() = "$media_url:large"
    val origUrl: String
        get() = "$media_url:orig"
}

@Parcelize
data class VideoInfo(
    val aspect_ratio: List<Int>,
    val duration_millis: Int,
    val variants: List<VideoVariant>
) : Parcelable {
    val mp4All: List<VideoVariant>
        get() = variants.filter { it.isMP4 }.sortedByDescending { it.bitrate }
    val mp4High: VideoVariant
        get() = mp4All.first()
    val mp4Mid: VideoVariant
        get() = mp4All.getOrElse(1) { mp4High }
    val mp4Small: VideoVariant
        get() = mp4All.getOrElse(2) { mp4Mid }
}

@Parcelize
data class VideoVariant(
    val bitrate: Int,
    val content_type: String,
    val url: String
) : Parcelable {
    companion object {
        const val TYPE_MP4 = "video/mp4"
    }

    val isMP4: Boolean
        get() = content_type == TYPE_MP4
}

data class UploadResult(
    val media_id: Long,
    val media_id_string: String,
    val size: Long,
    val expires_after_secs: Int?,
    val image: UploadImage?,
    val video: UploadVideo?,
    val processingInfo: ProcessingInfo?
)

data class UploadImage(
    val w: Int,
    val h: Int,
    val image_type: String
)

data class UploadVideo(
    val video_type: String
)

data class ProcessingInfo(
    val state: String,
    val check_after_secs: Int,
    val progress_percent: Int?,
    val error: ProcessingError?
)

data class ProcessingError(
    val code: Int,
    val name: String,
    val message: String
)
