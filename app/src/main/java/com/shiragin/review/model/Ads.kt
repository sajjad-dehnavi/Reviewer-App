package com.shiragin.review.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ads(
    @SerialName("isShow")
    val isShow: Boolean?,

    @SerialName("banner")
    val banner: String?,

    @SerialName("link")
    val link: String?,

    @SerialName("aspectRatio")
    val aspectRatio: Float?
)