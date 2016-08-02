package net.yusukezzz.ssmtc.data.json

import com.google.gson.annotations.SerializedName
import nz.bradcampbell.paperparcel.PaperParcel

@PaperParcel
data class User(
    val id: Long,
    val name: String,
    @SerializedName("screen_name") val screenName: String,
    @SerializedName("protected") val isProtected: Boolean,
    @SerializedName("verified") val isVerified: Boolean,
    @SerializedName("profile_image_url") val profileImageUrl: String,
    @SerializedName("profile_image_url_https") val profileImageUrlHttps: String,
    @SerializedName("statuses_count") val statusesCount: Long,
    @SerializedName("favourites_count") val favouritesCount: Long,
    @SerializedName("followers_count") val followersCount: Long,
    @SerializedName("friends_count") val friendsCount: Long,
    @SerializedName("listed_count") val listedCount: Long
)

