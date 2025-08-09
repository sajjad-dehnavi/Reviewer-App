package com.shiragin.libraries.review.internet.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class IpInfo(
    @SerialName("country") val country: String,
    @SerialName("countryCode") val countryCode: String
) {
    val isIran: Boolean get() = countryCode.lowercase() == "ir"
}