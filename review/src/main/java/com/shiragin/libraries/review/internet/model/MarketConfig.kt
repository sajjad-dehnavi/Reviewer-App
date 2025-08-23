package com.shiragin.libraries.review.internet.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MarketConfig(
    @SerialName("isShowAdmob")
    val isShowAdmob: Boolean,

    @SerialName("isShowTapsell")
    val isShowTapsell: Boolean,

    @SerialName("isHardVpnIfIran")
    val isHardVpnIfIran: Boolean,

    @SerialName("isHardReview")
    val isHardReview: Boolean,

    @SerialName("isSoftReview")
    val isSoftReview: Boolean,

    @SerialName("ads")
    val ads: List<Ads>? = null,
) {
    companion object {
        val DEFAULT = MarketConfig(
            isShowAdmob = true,
            isShowTapsell = true,
            isHardVpnIfIran = true,
            isHardReview = false,
            isSoftReview = false,
            ads = emptyList(),
        )
    }
}
