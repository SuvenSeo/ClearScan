package com.ardeno.clearscan.vault

data class PrivacyStatus(
    val networkPolicy: String,
    val storageLocation: String,
    val encryptionAtRestEnabled: Boolean,
    val encryptionHealthDetails: String = "AES-GCM 256-bit via Android Keystore",
    val adSdkFree: Boolean,
    val adSdkNotes: String,
    val systemBackupExcluded: Boolean,
    val exportAuditEntries: List<ExportAuditEntry> = emptyList(),
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L
)
