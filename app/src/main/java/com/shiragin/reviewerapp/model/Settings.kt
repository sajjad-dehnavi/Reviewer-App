package com.shiragin.reviewerapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    @SerialName("isShowLowCompression")
    val isShowLowCompression: Boolean = true,

    @SerialName("vpn")
    val vpn: Vpn? = null,

    @SerialName("ads")
    val ads: Ads? = null,
) {
    companion object {
        val DEFAULT = Settings(isShowLowCompression = true, vpn = null, ads = null)
    }
}