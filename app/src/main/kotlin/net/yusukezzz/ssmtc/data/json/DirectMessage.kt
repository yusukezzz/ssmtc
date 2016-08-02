package net.yusukezzz.ssmtc.data.json

import org.joda.time.DateTime

data class DirectMessage(
    val id: Long,
    val id_str: String,
    val recipient: User,
    val sender: User,
    val text: String,
    val created_at: DateTime
)
