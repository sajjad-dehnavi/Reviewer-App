package com.shiragin.libraries.review.internet.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
)