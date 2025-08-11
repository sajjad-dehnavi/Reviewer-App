package com.shiragin.review.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    @SerialName("isShowLowCompression")
    val isShowLowCompression: Boolean,

    @SerialName("vpn")
    val vpn: Vpn?,

    @SerialName("ads")
    val ads: Ads?
) {
    companion object {
        val DEFAULT = Settings(isShowLowCompression = true, vpn = null, ads = null)
    }
}