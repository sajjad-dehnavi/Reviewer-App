package com.shiragin.libraries.review

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object VersioningManager {

    fun versionChecker(
        currentVersionCode: Int,
        onUpdateAvailable: (isHardUpdate: Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val versioning = DataManager.getVersioning()
            if (currentVersionCode < versioning.lastHardUpdateVersionCode)
                onUpdateAvailable(true)
            else if (currentVersionCode < versioning.lastSoftUpdateVersionCode)
                onUpdateAvailable(false)
        }
    }
}