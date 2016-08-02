package net.yusukezzz.ssmtc.data

import net.yusukezzz.ssmtc.data.json.User
import net.yusukezzz.ssmtc.services.TimelineParameter
import nz.bradcampbell.paperparcel.PaperParcel

@PaperParcel
data class Account(
    val accessToken: String,
    val secretToken: String,
    val user: User,
    var timelines: List<TimelineParameter>,
    var lastTimelineIndex: Int
)
