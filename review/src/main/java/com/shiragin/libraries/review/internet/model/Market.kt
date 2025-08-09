package com.shiragin.libraries.review.internet.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Market(

    @SerialName("googlePlay")
    val googlePlay: MarketConfig,

    @SerialName("bazaar")
    val bazaar: MarketConfig,

    @SerialName("myket")
    val myket: MarketConfig
) {
    companion object {
        val DEFAULT = Market(
            googlePlay = MarketConfig.DEFAULT,
            bazaar = MarketConfig.DEFAULT,
            myket = MarketConfig.DEFAULT
        )
    }
}