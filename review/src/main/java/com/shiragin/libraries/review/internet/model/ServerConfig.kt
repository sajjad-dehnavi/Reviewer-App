package com.shiragin.libraries.review.internet.model


import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ServerConfig(
    @SerialName("settings")
    val settings: JsonObject? = null,

    @SerialName("market")
    val market: Market = Market.DEFAULT,

    @SerialName("versioning")
    val versioning: Versioning = Versioning.DEFAULT,

    @SerialName("vpn")
    val vpn: Vpn = Vpn.DEFAULT,

    @SerialName("ads")
    val ads: Ads = Ads.DEFAULT,

)