package net.yusukezzz.ssmtc.data.api.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TwList(
    val id: Long,
    @SerializedName("full_name") val fullName: String
) : Parcelable

data class TwLists(
    val lists: List<TwList>
)
