package com.shiragin.libraries.review.internet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ads(
    @SerialName("isShow")
    val isShow: Boolean?,

    @SerialName("isDefault")
    val isDefault: Boolean?,

    @SerialName("banner")
    val banner: String?,

    @SerialName("language")
    val language: String?,

    @SerialName("link")
    val link: String?,

    @SerialName("aspectRatio")
    val aspectRatio: Float?
) {
    companion object {
        val DEFAULT = Ads(null, null, null, null, null, null)
    }
}