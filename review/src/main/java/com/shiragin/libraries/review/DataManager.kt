package com.shiragin.libraries.review

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.shiragin.libraries.review.internet.RetrofitClient
import com.shiragin.libraries.review.internet.api.ConfigApi
import com.shiragin.libraries.review.internet.apiCall
import com.shiragin.libraries.review.internet.model.Ads
import com.shiragin.libraries.review.internet.model.MarketConfig
import com.shiragin.libraries.review.internet.model.ServerConfig
import com.shiragin.libraries.review.internet.model.Versioning
import com.shiragin.libraries.review.internet.model.Vpn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object DataManager {

    // region Constants
    private const val PREFS_NAME = "PixelPressPreferences"

    private const val PKG_GOOGLE_PLAY = "com.android.vending"
    private const val PKG_BAZAAR = "com.farsitel.bazaar"
    private const val PKG_MYKET = "ir.mservices.market"
    // endregion

    // region Preference Keys
    private val KEY_SERVER_CONFIG = stringPreferencesKey("server_config")
    private val KEY_IS_IN_IRAN = booleanPreferencesKey("is_in_iran")
    private val KEY_IGNORE_VPN = booleanPreferencesKey("ignoreVPN")
    // endregion

    // region Properties
    private lateinit var dataStore: DataStore<Preferences>
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val configApi: ConfigApi by lazy { RetrofitClient.retrofit.create(ConfigApi::class.java) }

    private var marketUrl: String = ""
    private var vendingPackageName: String = PKG_GOOGLE_PLAY
    private var isUserLocatedAtIran: Boolean = false
    // endregion

    // region Init
    fun init(
        context: Context,
        tapsellKey: String,
        testDeviceIdsForAdmob: List<String> = emptyList(),
        serverConfigUrl: String,
        marketUrl: String,
        vendingPackageName: String,
        iranLocationValidator: (Boolean) -> Unit
    ) {
        Reviewer.init(context)
        AdManager.init(context, tapsellKey, testDeviceIdsForAdmob)

        if (!::dataStore.isInitialized) {
            dataStore = PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                migrations = listOf(SharedPreferencesMigration(context, PREFS_NAME)),
                scope = ioScope,
                produceFile = { context.preferencesDataStoreFile(PREFS_NAME) }
            )
        }

        this.marketUrl = marketUrl
        this.vendingPackageName = vendingPackageName

        ioScope.launch {
            fetchServerConfig(context, serverConfigUrl)
            checkIranLocation(context, iranLocationValidator)
        }
    }
    // endregion

    // region Network Calls
    private suspend fun fetchServerConfig(context: Context, url: String) {
        apiCall(
            context = context,
            block = {
                Log.d("TAG_NET", "Fetching server config...")
                configApi.getServerConfig(url)
            },
            onSuccess = {
                Log.d("TAG_NET", "Server config received")
                savePreference(KEY_SERVER_CONFIG, Json.encodeToString(it))
            },
            onError = { Log.d("TAG_NET", "fetchServerConfig error: $it") }
        )
    }

    private suspend fun checkIranLocation(
        context: Context,
        validator: (Boolean) -> Unit
    ) {
        if (getPreference(KEY_IS_IN_IRAN, false)) {
            isUserLocatedAtIran = true
            validator(true)
            return
        }

        apiCall(
            context = context,
            block = { configApi.getIp() },
            onSuccess = { ipInfo ->
                if (ipInfo.isIran) {
                    isUserLocatedAtIran = true
                    setUserLocatedAtIran()
                    validator(true)
                } else {
                    validator(false)
                }
            },
            onError = { validator(false) }
        )
    }
    // endregion

    // region Market Checks
    private fun isGooglePlay() = vendingPackageName == PKG_GOOGLE_PLAY
    private fun isBazaar() = vendingPackageName == PKG_BAZAAR
    private fun isMyket() = vendingPackageName == PKG_MYKET
    // endregion

    // region Data Access
    suspend fun getServerConfig(): ServerConfig {
        val json = getPreference(KEY_SERVER_CONFIG, "")
        return json.let { Json.decodeFromString(it) } ?: ServerConfig()
    }

    fun getServerConfigFlow(): Flow<ServerConfig> {
        val json = getPreferenceFlow(KEY_SERVER_CONFIG, "")
        return json.map { Json.decodeFromString(it) ?: ServerConfig() }
    }

    suspend fun isUserLocatedAtIran(): Boolean = getPreference(KEY_IS_IN_IRAN, false)

    suspend fun isIgnoreVpn(): Boolean = getPreference(KEY_IGNORE_VPN, false)

    suspend fun setUserLocatedAtIran() = savePreference(KEY_IS_IN_IRAN, true)

    suspend fun setIgnoreVpn(ignore: Boolean) = savePreference(KEY_IGNORE_VPN, ignore)

    suspend fun getVersioning(): Versioning = getServerConfig().versioning

    suspend fun getVersioningFlow(): Flow<Versioning> = getServerConfigFlow().map { it.versioning }

    suspend fun getAds(): Ads = getServerConfig().ads

    suspend fun getAdsFlow(): Flow<Ads> = getServerConfigFlow().map { it.ads }

    suspend fun getVpn(): Vpn = getServerConfig().vpn

    suspend fun getVpnFlow(): Flow<Vpn> = getServerConfigFlow().map { it.vpn }

    suspend inline fun <reified T> getSettings(): T? {
        val settingsJsonObject: JsonObject = getServerConfig().settings ?: return null
        return try {
            val decoder = Json { ignoreUnknownKeys = true }
            decoder.decodeFromJsonElement<T>(settingsJsonObject)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified T> getSettingsFlow(): Flow<T?> {
        val settingsJsonObject: Flow<JsonObject?> = getServerConfigFlow().map { it.settings }
        return try {
            val decoder = Json { ignoreUnknownKeys = true }
            settingsJsonObject.map { json ->
                json?.let { decoder.decodeFromJsonElement<T>(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            flow { null }
        }
    }

    fun getMarketUrl(): String = marketUrl

    fun getVendingPackageName(): String = vendingPackageName

    suspend fun getMarketConfig(): MarketConfig = when {
        isGooglePlay() -> getServerConfig().market.googlePlay
        isBazaar() -> getServerConfig().market.bazaar
        isMyket() -> getServerConfig().market.myket
        else -> MarketConfig.DEFAULT
    }
// endregion

    // region Preferences Helpers
    private fun <T> getPreferenceFlow(key: Preferences.Key<T>, default: T) =
        dataStore.data.map { it[key] ?: default }

    private suspend fun <T> getPreference(key: Preferences.Key<T>, default: T): T =
        getPreferenceFlow(key, default).first()

    private suspend fun <T> savePreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
// endregion
}