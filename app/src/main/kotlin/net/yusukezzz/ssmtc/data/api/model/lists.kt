package net.yusukezzz.ssmtc.data.api.model

import com.google.gson.annotations.SerializedName
import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

@PaperParcel
data class TwList(
    val id: Long,
    @SerializedName("full_name") val fullName: String
): PaperParcelable {
    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(TwList::class.java)
    }
}

data class TwLists(
    val lists: List<TwList>
)
