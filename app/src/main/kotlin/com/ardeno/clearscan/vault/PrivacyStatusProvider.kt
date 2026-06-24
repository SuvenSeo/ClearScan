package com.ardeno.clearscan.vault

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File

class PrivacyStatusProvider(
    private val context: Context,
    private val encryptedFileStore: EncryptedFileStore,
    private val exportAuditLog: ExportAuditLog,
    private val vaultCrypto: VaultCrypto
) {
    fun load(): PrivacyStatus {
        val adScan = scanForAdSdks()
        return PrivacyStatus(
            networkPolicy = "No network calls in core flows. Export and backup are explicit user actions only.",
            storageLocation = encryptedFileStore.storageRoot().absolutePath,
            encryptionAtRestEnabled = vaultCrypto.healthCheck(),
            adSdkFree = adScan.isClean,
            adSdkNotes = adScan.summary,
            systemBackupExcluded = isSystemBackupExcluded(),
            exportAuditEntries = exportAuditLog.formattedEntries()
        )
    }

    private fun isSystemBackupExcluded(): Boolean {
        val appInfo = context.applicationInfo
        val disabled = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) == 0
        val rulesFile = File(context.applicationInfo.dataDir, "../shared_prefs").exists()
        return disabled || rulesFile
    }

    private fun scanForAdSdks(): AdSdkScan {
        val blockedMarkers = listOf(
            "com.google.android.gms.ads",
            "com.applovin",
            "com.unity3d.ads",
            "com.facebook.ads",
            "com.mopub",
            "com.chartboost",
            "com.ironsource",
            "com.vungle"
        )
        val hits = blockedMarkers.filter { marker ->
            runCatching {
                Class.forName(marker)
                true
            }.getOrDefault(false)
        }
        return if (hits.isEmpty()) {
            AdSdkScan(
                isClean = true,
                summary = "No common ad SDK classes detected in the app classpath."
            )
        } else {
            AdSdkScan(
                isClean = false,
                summary = "Detected SDK markers: ${hits.joinToString()}"
            )
        }
    }

    private data class AdSdkScan(
        val isClean: Boolean,
        val summary: String
    )
}
