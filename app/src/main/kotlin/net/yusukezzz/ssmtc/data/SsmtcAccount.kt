package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.model.User
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@PaperParcel
data class Credentials(val token: String, val tokenSecret: String) : PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelCredentials.CREATOR
    }
}

@PaperParcel
data class SsmtcAccount(
    val credentials: Credentials,
    val user: User,
    var timelines: List<TimelineParameter>,
    var currentTimelineUuid: UUID
): PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelSsmtcAccount.CREATOR
    }

    fun currentTimeline(): TimelineParameter = timelines.find { it.uuid == currentTimelineUuid }!!
    fun withoutCurrentTimeline(): SsmtcAccount {
        val newTimelines = timelines.filterNot { it.uuid == currentTimelineUuid }
        return this.copy(timelines = newTimelines, currentTimelineUuid = newTimelines.first().uuid)
    }
}
