package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.model.User
import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

@PaperParcel
data class Account(
    val accessToken: String,
    val secretToken: String,
    val user: User,
    var timelines: List<TimelineParameter>,
    var lastTimelineIndex: Int
): PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Account::class.java)
    }
}
