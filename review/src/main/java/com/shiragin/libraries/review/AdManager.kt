package com.shiragin.libraries.review

import android.app.Activity
import android.app.Application.getProcessName
import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.WebView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.shiragin.libraries.review.errors.InternetConnectionError
import com.shiragin.libraries.review.internet.RetrofitClient
import com.shiragin.libraries.review.internet.api.ConfigApi
import com.shiragin.libraries.review.internet.apiCall
import com.shiragin.libraries.review.internet.model.MarketConfig
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdManager {

    private val configApi by lazy { RetrofitClient.retrofit.create(ConfigApi::class.java) }
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun init(context: Context, tapsellKey: String, testDeviceIds: List<String> = emptyList()) {
        initWebViewForMultipleProcesses(context)
        initTapsell(context, tapsellKey)
        initAdMob(context, testDeviceIds)
    }

    // ---------------- Init ----------------

    private fun initWebViewForMultipleProcesses(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()?.takeIf { it != context.packageName }?.let {
                WebView.setDataDirectorySuffix(it)
            }
        }
    }

    private fun initTapsell(context: Context, key: String) {
        TapsellPlus.initialize(context, key, object : TapsellPlusInitListener {
            override fun onInitializeSuccess(networks: AdNetworks) {
                TapsellPlus.setGDPRConsent(context, true)
                Log.d("AdManager", "Tapsell initialized: ${networks.name}")
            }

            override fun onInitializeFailed(networks: AdNetworks, error: AdNetworkError) {
                Log.e("AdManager", "Tapsell init failed: ${networks.name}, ${error.errorMessage}")
            }
        })
    }

    private fun initAdMob(context: Context, testDevices: List<String>) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
        )
        MobileAds.initialize(context)
    }

    // ---------------- Public API ----------------
    fun requestRewardedInterstitialAd(
        activity: Activity,
        admobId: String,
        onStart: () -> Unit,
        onLoaded: (RewardedInterstitialAd) -> Unit,
        onError: (Exception) -> Unit
    ) = mainScope.launch {
        onStart()
        checkCountryAndLoadRewardedInterstitial(activity, admobId, onLoaded, onError)
    }

    fun requestInterstitialAd(
        activity: Activity,
        admobId: String,
        onStart: () -> Unit,
        onLoaded: (InterstitialAd) -> Unit,
        onError: (Exception) -> Unit
    ) = mainScope.launch {
        onStart()
        checkCountryAndLoadInterstitial(activity, admobId, onLoaded, onError)
    }

    fun requestRewardAd(
        activity: Activity,
        admobId: String,
        tapsellId: String,
        onStart: () -> Unit,
        onCountryDetected: (String?, Boolean) -> Unit,
        onVpnNeeded: () -> Unit,
        onTurnOffVpn: () -> Unit = {},
        onEarned: (String) -> Unit,
        onClose: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) = mainScope.launch {
        onStart()
        val market = DataManager.getMarketConfig()
        val ignoreVpn = DataManager.isIgnoreVpn()
        val iranLocated = DataManager.isUserLocatedAtIran()

        apiCall(
            context = activity,
            block = { configApi.getIp() },
            onSuccess = { ip ->
                if (ip.isIran) DataManager.setUserLocatedAtIran()
                onCountryDetected(ip.country.lowercase(), market.isHardVpnIfIran)

                when {
                    ip.isIran && market.isHardVpnIfIran && !ignoreVpn -> onVpnNeeded()
                    ip.isIran && !market.isHardVpnIfIran -> loadTapsellIfAllowed(
                        activity,
                        tapsellId,
                        market,
                        onEarned,
                        onError,
                        onClose
                    )

                    else -> loadNonIranAds(
                        activity,
                        admobId,
                        tapsellId,
                        market,
                        ignoreVpn,
                        iranLocated,
                        onEarned,
                        onTurnOffVpn,
                        onError,
                        onClose
                    )
                }
            },
            onError = { err ->
                onCountryDetected(null, market.isHardVpnIfIran)
                if (err is InternetConnectionError) return@apiCall onError(err)
                handleErrorLoading(activity, admobId, tapsellId, market, onEarned, onError, onClose)
            }
        )
    }

    // ---------------- Private helpers ----------------

    private suspend fun checkCountryAndLoadRewardedInterstitial(
        activity: Activity,
        admobId: String,
        onLoaded: (RewardedInterstitialAd) -> Unit,
        onError: (Exception) -> Unit
    ) {
        apiCall(
            context = activity,
            block = { configApi.getIp() },
            onSuccess = { ip ->
                if (!ip.isIran) {
                    DataManager.setUserLocatedAtIran()
                    loadAdmobRewardedInterstitial(activity, admobId, onLoaded, onError)
                } else onError(Exception("country is iran"))
            },
            onError = { loadAdmobRewardedInterstitial(activity, admobId, onLoaded, onError) }
        )
    }

    private suspend fun checkCountryAndLoadInterstitial(
        activity: Activity,
        admobId: String,
        onLoaded: (InterstitialAd) -> Unit,
        onError: (Exception) -> Unit
    ) {
        apiCall(
            context = activity,
            block = { configApi.getIp() },
            onSuccess = { ip ->
                if (!ip.isIran) {
                    DataManager.setUserLocatedAtIran()
                    loadAdmobInterstitial(activity, admobId, onLoaded, onError)
                } else onError(Exception("country is iran"))
            },
            onError = { loadAdmobInterstitial(activity, admobId, onLoaded, onError) }
        )
    }


    private fun loadAdmobRewardedInterstitial(
        activity: Activity,
        id: String,
        onLoaded: (RewardedInterstitialAd) -> Unit,
        onError: (Exception) -> Unit
    ) {
        RewardedInterstitialAd.load(
            activity, id, AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(err: LoadAdError) = onError(Exception(err.message))
                override fun onAdLoaded(ad: RewardedInterstitialAd) = onLoaded(ad)
            }
        )
    }

    private fun loadAdmobInterstitial(
        activity: Activity,
        id: String,
        onLoaded: (InterstitialAd) -> Unit,
        onError: (Exception) -> Unit
    ) {
        InterstitialAd.load(
            activity, id, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(err: LoadAdError) = onError(Exception(err.message))
                override fun onAdLoaded(ad: InterstitialAd) = onLoaded(ad)
            }
        )
    }


    private fun loadTapsellIfAllowed(
        activity: Activity,
        tapsellId: String,
        market: MarketConfig,
        onEarned: (String) -> Unit,
        onError: (Exception) -> Unit,
        onClose: (Boolean) -> Unit
    ) {
        if (market.isShowTapsell) {
            requestTapsell(activity, tapsellId, onEarned, onError, onClose)
        } else onEarned("fail_isShowTapsell == false")
    }

    private fun loadNonIranAds(
        activity: Activity,
        admobId: String,
        tapsellId: String,
        market: MarketConfig,
        ignoreVpn: Boolean,
        iranLocated: Boolean,
        onEarned: (String) -> Unit,
        onTurnOffVpn: () -> Unit,
        onError: (Exception) -> Unit,
        onClose: (Boolean) -> Unit
    ) {
        if (!market.isShowAdmob) return loadTapsellIfAllowed(
            activity,
            tapsellId,
            market,
            onEarned,
            onError,
            onClose
        )

        requestAdmob(
            activity = activity,
            id = admobId,
            onEarned = onEarned,
            onClose = onClose
        ) {
            requestTapsell(
                activity = activity,
                id = tapsellId,
                onEarned = onEarned,
                onError = { inner ->
                    when {
                        ignoreVpn -> onTurnOffVpn()
                        iranLocated -> {
                            mainScope.launch { DataManager.setIgnoreVpn(true) }
                            onEarned("fail_load_admob_and_tapsell == false")
                        }

                        else -> onEarned("fail_load_admob_and_tapsell == false")
                    }
                    onError(inner)
                },
                onClose = onClose,
            )
        }
    }

    private fun handleErrorLoading(
        activity: Activity,
        admobId: String,
        tapsellId: String,
        market: MarketConfig,
        onEarned: (String) -> Unit,
        onError: (Exception) -> Unit,
        onClose: (Boolean) -> Unit
    ) {
        when {
            market.isShowAdmob -> requestAdmob(activity, admobId, onEarned, onClose) {
                if (market.isShowTapsell) requestTapsell(
                    activity,
                    tapsellId,
                    onEarned,
                    onError,
                    onClose
                )
                else onError(it)
            }

            market.isShowTapsell -> requestTapsell(activity, tapsellId, onEarned, onError, onClose)
            else -> onError(Exception("No ad network available"))
        }
    }

    // ---------------- Admob ----------------

    private fun requestAdmob(
        activity: Activity,
        id: String,
        onEarned: (String) -> Unit,
        onClose: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        RewardedAd.load(
            activity, id, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(err: LoadAdError) =
                    onError(Exception("fail_load_admob_${err.message}"))

                override fun onAdLoaded(ad: RewardedAd) =
                    showAdmob(activity, ad, onEarned, onClose, onError)
            }
        )
    }

    private fun showAdmob(
        activity: Activity,
        ad: RewardedAd,
        onEarned: (String) -> Unit,
        onClose: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = onClose(rewarded)
            override fun onAdFailedToShowFullScreenContent(err: AdError) =
                onError(Exception("fail_show_admob_${err.message}"))
        }
        ad.show(activity) {
            rewarded = true
            onEarned("Admob")
        }
    }

    // ---------------- Tapsell ----------------

    private fun requestTapsell(
        activity: Activity,
        id: String,
        onEarned: (String) -> Unit,
        onError: (Exception) -> Unit,
        onClose: (Boolean) -> Unit
    ) {
        TapsellPlus.requestRewardedVideoAd(activity, id, object : AdRequestCallback() {
            override fun response(model: TapsellPlusAdModel) {
                if (!activity.isDesroyed) showTapsell(activity, model, onEarned, onClose, onError)
            }

            override fun error(msg: String) =
                if (!activity.isDestroyed) onError(Exception("fail_load_tapsell_$msg")) else Unit
        })
    }

    private fun showTapsell(
        activity: Activity,
        model: TapsellPlusAdModel,
        onEarned: (String) -> Unit,
        onClose: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        var rewarded = false
        TapsellPlus.showRewardedVideoAd(activity, model.responseId, object : AdShowListener() {
            override fun onClosed(m: TapsellPlusAdModel) = onClose(rewarded)
            override fun onRewarded(m: TapsellPlusAdModel) {
                rewarded = true; onEarned("Tapsell")
            }

            override fun onError(e: TapsellPlusErrorModel) =
                onError(Exception("fail_show_tapsell_${e.errorMessage}"))
        })
    }
}