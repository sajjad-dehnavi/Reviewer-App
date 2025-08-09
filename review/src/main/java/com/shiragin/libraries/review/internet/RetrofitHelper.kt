package com.shiragin.libraries.review.internet

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.shiragin.libraries.review.errors.InternetConnectionError

internal suspend fun <T> apiCall(
    context: Context,
    block: suspend () -> T,
    onSuccess: suspend (T) -> Unit,
    onError: (Exception) -> Unit,
) {
    if (context.isNetworkConnected) {
        runCatching {
            block()
        }.onSuccess { value ->
            onSuccess(value)
        }.onFailure { exception ->
            onError(Exception(exception))
        }
    } else {
        onError(InternetConnectionError())
    }
}


private val Context.isNetworkConnected: Boolean
    get() = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isCurrentlyConnected()


private fun ConnectivityManager?.isCurrentlyConnected() = when (this) {
    null -> false
    else -> when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            activeNetwork
                ?.let(::getNetworkCapabilities)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ?: false

        else -> activeNetworkInfo?.isConnected ?: false
    }
}