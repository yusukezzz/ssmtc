package net.yusukezzz.ssmtc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.yusukezzz.ssmtc.data.api.Timeline
import net.yusukezzz.ssmtc.data.api.model.User
import java.util.*

@Parcelize
data class Credentials(val token: String, val tokenSecret: String) : Parcelable

@Parcelize
data class SsmtcAccount(
    val credentials: Credentials,
    val user: User,
    var timelines: List<Timeline>,
    var currentTimelineUuid: UUID
) : Parcelable {
    fun currentTimeline(): Timeline = timelines.find { it.uuid == currentTimelineUuid }!!
    fun withoutCurrentTimeline(): SsmtcAccount {
        val newTimelines = timelines.filterNot { it.uuid == currentTimelineUuid }
        return this.copy(timelines = newTimelines, currentTimelineUuid = newTimelines.first().uuid)
    }
}
