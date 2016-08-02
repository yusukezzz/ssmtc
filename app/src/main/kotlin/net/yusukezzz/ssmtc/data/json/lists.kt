package net.yusukezzz.ssmtc.data.json

import com.google.gson.annotations.SerializedName
import nz.bradcampbell.paperparcel.PaperParcel

@PaperParcel
data class TwList(
    val id: Long,
    @SerializedName("full_name") val fullName: String
)

data class TwLists(
    val lists: List<TwList>
)
