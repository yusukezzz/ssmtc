package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.model.User
import paperparcel.PaperParcel
import paperparcel.PaperParcelable

@PaperParcel
data class Account(
    val accessToken: String,
    val secretToken: String,
    val user: User,
    var timelines: List<TimelineParameter>,
    var lastTimelineIndex: Int
): PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelAccount.CREATOR
    }
}
