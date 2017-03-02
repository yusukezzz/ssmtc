package net.yusukezzz.ssmtc.data.api.model

import com.google.gson.annotations.SerializedName
import paperparcel.PaperParcel
import paperparcel.PaperParcelable

@PaperParcel
data class TwList(
    val id: Long,
    @SerializedName("full_name") val fullName: String
): PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelTwList.CREATOR
    }
}

data class TwLists(
    val lists: List<TwList>
)
