package com.shiragin.libraries.review.internet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vpn(
    @SerialName("title")
    val title: String?,

    @SerialName("icon")
    val icon: String?,

    @SerialName("link")
    val link: String?
) {
    companion object {
        val DEFAULT = Vpn(title = null, icon = null, link = null)
    }
}