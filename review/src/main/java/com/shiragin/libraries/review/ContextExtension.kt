package com.shiragin.libraries.review

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

internal fun Context.openMarketForRating(
    packageName: String,
    marketUrl: String,
    vendingPackageName: String
) {
    try {
        val intent = Intent(
            Intent.ACTION_EDIT,
        ).apply {
            data = "market://details?id=$packageName".toUri()
            setPackage(vendingPackageName)
        }

        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(marketUrl)
            )
        )
    }
}