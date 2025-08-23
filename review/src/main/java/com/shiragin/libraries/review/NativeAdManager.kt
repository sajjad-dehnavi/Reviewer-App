package com.shiragin.libraries.review

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.shiragin.libraries.review.errors.InternetConnectionError
import com.shiragin.libraries.review.internet.RetrofitClient
import com.shiragin.libraries.review.internet.api.ConfigApi
import com.shiragin.libraries.review.internet.apiCall
import com.shiragin.libraries.review.model.NativeAdModel
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.model.TapsellPlusAdModel


@SuppressLint("MissingPermission")
@Composable
fun SubscribeForAd(
    context: Activity,
    admobId: String,
    tapsellId: String,
    onAdLoaded: (NativeAdModel) -> Unit,
    onAdFailedToLoad: (error: String?) -> Unit,
    onError: (Exception) -> Unit,
) {
    var adProvider by remember { mutableStateOf<AdProvider?>(null) }
    var hasRunOnce by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasRunOnce) return@LaunchedEffect
        hasRunOnce = true

        val marketConfig = DataManager.getMarketConfig()
        val configApi = RetrofitClient.retrofit.create(ConfigApi::class.java)
        val hardVpn = marketConfig.isHardVpnIfIran
        val showTapsell = marketConfig.isShowTapsell
        val showAdmob = marketConfig.isShowAdmob

        apiCall(
            context = context,
            block = { configApi.getIp() },
            onSuccess = { ipInfo ->
                val isIran = ipInfo.isIran
                if (isIran) DataManager.setUserLocatedAtIran()

                when {
                    isIran && !hardVpn -> {
                        adProvider = if (!showTapsell) {
                            null
                        } else {
                            AdProvider.TAPSELL
                        }
                    }

                    !showAdmob -> {
                        adProvider = if (!showTapsell) {
                            null
                        } else {
                            AdProvider.TAPSELL
                        }
                    }

                    else -> {
                        adProvider = AdProvider.AdMob
                    }
                }
            },
            onError = { exception ->
                if (exception is InternetConnectionError) {
                    onError(exception)
                    adProvider = null
                } else if (showAdmob) {
                    adProvider = AdProvider.AdMob
                } else if (showTapsell) {
                    adProvider = AdProvider.TAPSELL
                } else {
                    adProvider = null
                }
            }
        )
    }

    LaunchedEffect(adProvider) {
        if (adProvider == null) return@LaunchedEffect

        when (adProvider) {
            AdProvider.AdMob -> {
                val adLoader = AdLoader.Builder(context, admobId)
                    .forNativeAd { ad ->
                        onAdLoaded(
                            NativeAdModel(
                                admobNativeAd = ad,
                                adProvider = AdProvider.AdMob,
                            )
                        )
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            onAdFailedToLoad(error.message)
                        }
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            }

            AdProvider.TAPSELL -> {
                TapsellPlus.requestNativeAd(context, tapsellId, object : AdRequestCallback() {
                    override fun response(tapsellPlusAdModel: TapsellPlusAdModel) {
                        val tapsellStandardBannerResponseId =
                            tapsellPlusAdModel.responseId
                        onAdLoaded(
                            NativeAdModel(
                                tapsellResponseId = tapsellStandardBannerResponseId,
                                adProvider = AdProvider.TAPSELL,
                            )
                        )
                    }

                    override fun error(s: String?) {
                        onAdFailedToLoad(s)
                    }
                })
            }

            else -> {}
        }
    }
}