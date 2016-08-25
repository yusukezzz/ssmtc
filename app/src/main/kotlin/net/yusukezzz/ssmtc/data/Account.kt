package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.json.User
import net.yusukezzz.ssmtc.services.TimelineParameter
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
