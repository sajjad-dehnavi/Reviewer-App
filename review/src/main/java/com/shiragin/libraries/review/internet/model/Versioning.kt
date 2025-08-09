package com.shiragin.libraries.review.internet.model


import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Versioning(
    @SerialName("lastHardUpdateVersionCode")
    val lastHardUpdateVersionCode: Int = 1,
    @SerialName("lastSoftUpdateVersionCode")
    val lastSoftUpdateVersionCode: Int = 1
) {
    companion object {
        val DEFAULT = Versioning()
    }
}