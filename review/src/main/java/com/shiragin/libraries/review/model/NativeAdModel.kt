package com.shiragin.libraries.review.model

import com.google.android.gms.ads.nativead.NativeAd
import com.shiragin.libraries.review.AdProvider

data class NativeAdModel(
    val admobNativeAd: NativeAd? = null,
    val tapsellResponseId: String? = null,
    val adProvider: AdProvider? = null,
)