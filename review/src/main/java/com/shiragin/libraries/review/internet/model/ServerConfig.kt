package com.shiragin.libraries.review.internet.model


import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ServerConfig(
    @SerialName("settings")
    val settings: Settings = Settings.DEFAULT,

    @SerialName("market")
    val market: Market = Market.DEFAULT,

    @SerialName("versioning")
    val versioning: Versioning = Versioning.DEFAULT
)