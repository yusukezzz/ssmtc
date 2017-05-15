package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.api.TimelineParameter
import net.yusukezzz.ssmtc.data.api.model.User
import paperparcel.PaperParcel
import paperparcel.PaperParcelable

@PaperParcel
data class Credential(val token: String, val tokenSecret: String) : PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelCredential.CREATOR
    }
}

@PaperParcel
data class SsmtcAccount(
    val accessToken: String,
    val secretToken: String,
    val user: User,
    var timelines: List<TimelineParameter>,
    var lastTimelineIndex: Int
): PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelSsmtcAccount.CREATOR
    }
}
