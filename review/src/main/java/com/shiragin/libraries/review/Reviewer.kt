package com.shiragin.libraries.review

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.shiragin.libraries.review.internet.RetrofitClient
import com.shiragin.libraries.review.internet.api.ConfigApi
import com.shiragin.libraries.review.internet.apiCall
import com.shiragin.libraries.review.internet.model.Review
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object Reviewer {

    // region Constants & Keys
    private const val APP_PREFERENCES = "reviewer_app_open_tracker"

    private val KEY_APP_OPEN_COUNT = intPreferencesKey("count_of_app_open")
    private val KEY_USER_LEFT_REVIEW = booleanPreferencesKey("is_user_pressed_submit_button")
    // endregion

    // region Properties
    private lateinit var dataStore: DataStore<Preferences>
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val configApi: ConfigApi by lazy { RetrofitClient.retrofit.create(ConfigApi::class.java) }
    // endregion

    // region Initialization
    fun init(context: Context) {
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(SharedPreferencesMigration(context, APP_PREFERENCES)),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile(APP_PREFERENCES) }
        )
    }
    // endregion

    // region Public Methods
    fun incrementAppOpenCounter() = appScope.launch {
        savePreference(KEY_APP_OPEN_COUNT, getPreference(KEY_APP_OPEN_COUNT, 0) + 1)
    }

    suspend fun isNeedSoftReview(): Boolean =
        !getPreference(KEY_USER_LEFT_REVIEW, false) && DataManager.getMarketConfig().isSoftReview

    suspend fun isAllowHardReview(): Boolean =
        !getPreference(KEY_USER_LEFT_REVIEW, false) && DataManager.getMarketConfig().isHardReview

    fun openMarketForRating(context: Context) {
        context.openMarketForRating(
            packageName = context.packageName,
            marketUrl = DataManager.getMarketUrl(),
            vendingPackageName = DataManager.getVendingPackageName()
        )
    }

    fun submitButtonPressed() = appScope.launch {
        savePreference(KEY_USER_LEFT_REVIEW, true)
    }

    private fun googlePlayRateAndReview(context: Context) {
        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
            } else {
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
            }
        }
    }

    fun sendReview(
        context: Context,
        appName: String,
        versionName: String,
        comment: String,
        rate: Int,
        onStartSubmitting: () -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        onStartSubmitting()
        CoroutineScope(Dispatchers.Main).launch {
            apiCall(
                context = context,
                block = { configApi.getIp() },
                onSuccess = { ipInfo ->
                    if (ipInfo.isIran) DataManager.setUserLocatedAtIran()
                    apiCall(
                        context = context,
                        block = {
                            configApi.leaveComment(
                                review = Review(
                                    appName = appName,
                                    versionName = versionName,
                                    comment = comment,
                                    rate = rate,
                                    country = ipInfo.country
                                )
                            )
                        },
                        onSuccess = { onSuccess() },
                        onError = { onFailure(it) }
                    )
                },
                onError = { onFailure(it) }
            )
        }
    }
    // endregion

    // region Private Helpers
    private fun <T> getPreferenceFlow(key: Preferences.Key<T>, default: T) =
        dataStore.data.map { it[key] ?: default }

    private suspend fun <T> getPreference(key: Preferences.Key<T>, default: T): T =
        getPreferenceFlow(key, default).first()

    private suspend fun <T> savePreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
    // endregion
}