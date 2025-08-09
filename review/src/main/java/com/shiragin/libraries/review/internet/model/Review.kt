package com.shiragin.libraries.review.internet.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class Review(
    @SerialName("appName") val appName: String,
    @SerialName("versionName") val versionName: String,
    @SerialName("comment") val comment: String,
    @SerialName("rate") val rate: Int,
    @SerialName("country") val country: String,
)