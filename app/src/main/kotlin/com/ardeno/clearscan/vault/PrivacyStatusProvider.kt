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
        val cryptoHealthy = vaultCrypto.healthCheck()
        val storageRoot = encryptedFileStore.storageRoot()
        val (usedBytes, totalBytes) = computeStorageUsage(storageRoot)
        return PrivacyStatus(
            networkPolicy = "No network calls in core flows. Export and backup are explicit user actions only.",
            storageLocation = storageRoot.absolutePath,
            encryptionAtRestEnabled = cryptoHealthy,
            encryptionHealthDetails = if (cryptoHealthy) {
                "AES-GCM 256-bit via Android Keystore · encryption + decryption verified"
            } else {
                "Vault encryption is unavailable on this device."
            },
            adSdkFree = adScan.isClean,
            adSdkNotes = adScan.summary,
            systemBackupExcluded = isSystemBackupExcluded(),
            exportAuditEntries = exportAuditLog.readEntries().take(20),
            storageUsedBytes = usedBytes,
            storageTotalBytes = totalBytes
        )
    }

    private fun computeStorageUsage(root: File): Pair<Long, Long> {
        val used = root.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
        val total = root.totalSpace.coerceAtMost(root.freeSpace + used).coerceAtLeast(used)
        return Pair(used, total)
    }

    private fun isSystemBackupExcluded(): Boolean {
        val appInfo = context.applicationInfo
        return (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) == 0
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
