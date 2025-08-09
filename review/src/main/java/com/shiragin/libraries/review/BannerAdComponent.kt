package com.shiragin.libraries.review

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.shiragin.libraries.review.errors.InternetConnectionError
import com.shiragin.libraries.review.internet.RetrofitClient
import com.shiragin.libraries.review.internet.api.ConfigApi
import com.shiragin.libraries.review.internet.apiCall
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusBannerType
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel

internal sealed interface AdProvider {
    data object TAPSELL : AdProvider
    data object AdMob : AdProvider
}

@Composable
fun BannerAdComponent(
    modifier: Modifier = Modifier,
    activity: Activity,
    admobBannerId: String,
    tapsellBannerId: String,
    onCountryDetected: (country: String?, hardVpn: Boolean) -> Unit,
    onVpnNeeded: () -> Unit,
    onError: (Exception) -> Unit,
) {
    val adView = remember { AdView(activity) }
    val tapsellAdView = remember { LinearLayout(activity) }
    var adProvider by remember { mutableStateOf<AdProvider?>(null) }
    var showBanner by rememberSaveable { mutableStateOf(true) }
    var tapsellStandardBannerResponseId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasRunOnce by rememberSaveable { mutableStateOf(false) }

    // Fetch Country & Decide Ad Strategy Once
    LaunchedEffect(Unit) {
        if (hasRunOnce) return@LaunchedEffect
        hasRunOnce = true
        val marketConfig = DataManager.getMarketConfig()
        val configApi = RetrofitClient.retrofit.create(ConfigApi::class.java)
        val hardVpn = marketConfig.isHardVpnIfIran
        val showTapsell = marketConfig.isShowTapsell
        val showAdmob = marketConfig.isShowAdmob

        apiCall(
            context = activity,
            block = { configApi.getIp() },
            onSuccess = { ipInfo ->
                val isIran = ipInfo.isIran


                if (isIran) DataManager.setUserLocatedAtIran()
                onCountryDetected(ipInfo.country.lowercase(), hardVpn)

                when {
                    isIran && hardVpn -> onVpnNeeded()
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
                onCountryDetected(null, marketConfig.isHardVpnIfIran)
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

    // Setup AdMob Banner When Needed
    LaunchedEffect(adProvider) {
        if (adProvider == null) return@LaunchedEffect

        if (adProvider is AdProvider.AdMob) {
            adView.apply {
                adUnitId = admobBannerId
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360))
                adListener = object : AdListener() {
                    override fun onAdClosed() {

                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("BannerAdComponent", adError.message)
                        onError(Exception(adError.message))
                    }
                }
                loadAd(
                    AdRequest
                        .Builder()
                        .build()
                )
            }
        } else if (adProvider is AdProvider.TAPSELL) {
            TapsellPlus.requestStandardBannerAd(
                activity, tapsellBannerId,
                TapsellPlusBannerType.BANNER_320x100,
                object : AdRequestCallback() {
                    override fun response(tapsellPlusAdModel: TapsellPlusAdModel) {
                        super.response(tapsellPlusAdModel)
                        tapsellPlusAdModel.responseId?.let {
                            tapsellStandardBannerResponseId = it
                        }
                    }

                    override fun error(message: String?) {
                        super.error(message)
                        onError(Exception(message))
                    }
                })
        }
    }

    LaunchedEffect(tapsellStandardBannerResponseId) {
        if (tapsellStandardBannerResponseId != null) {
            TapsellPlus.showStandardBannerAd(
                activity, tapsellStandardBannerResponseId,
                tapsellAdView,
                object : AdShowListener() {

                    override fun onError(p0: TapsellPlusErrorModel?) {
                        super.onError(p0)
                        onError(Exception(p0?.errorMessage))
                    }
                })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adView.destroy()
                TapsellPlus.destroyStandardBanner(
                    activity,
                    tapsellStandardBannerResponseId,
                    tapsellAdView,
                )
            } catch (_: Exception) {
            }
        }
    }

    if (showBanner) {
        Box {
            if (adProvider is AdProvider.TAPSELL) {
                TapsellBannerAd(tapsellAdView, modifier.fillMaxWidth())
            } else if (adProvider is AdProvider.AdMob) {
                AdmobBannerAd(adView, modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AdmobBannerAd(adView: AdView, modifier: Modifier = Modifier) {
    // Ad load does not work in preview mode because it requires a network connection.
    if (LocalInspectionMode.current) {
        Box { Text(text = "Google Mobile Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    AndroidView(modifier = modifier.wrapContentSize(), factory = { adView })

    // Pause and resume the AdView when the lifecycle is paused and resumed.
    LifecycleResumeEffect(adView) {
        adView.resume()
        onPauseOrDispose { adView.pause() }
    }
}

@Composable
private fun TapsellBannerAd(
    adView: View,
    modifier: Modifier = Modifier
) {
    // Ad load does not work in preview mode because it requires a network connection.
    if (LocalInspectionMode.current) {
        Box { Text(text = "Tapsell Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    AndroidView(modifier = modifier.wrapContentSize(), factory = { adView })
}