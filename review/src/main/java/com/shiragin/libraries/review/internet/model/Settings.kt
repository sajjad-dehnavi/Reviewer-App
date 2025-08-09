package com.shiragin.libraries.review.internet.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Settings(
    @SerialName("limitChat")
    val limitChat: Int = 0,
) {
    companion object {
        val DEFAULT = Settings(limitChat = 0)
    }
}