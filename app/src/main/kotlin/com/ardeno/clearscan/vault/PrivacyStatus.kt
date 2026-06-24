package com.ardeno.clearscan.vault

data class PrivacyStatus(
    val networkPolicy: String,
    val storageLocation: String,
    val encryptionAtRestEnabled: Boolean,
    val adSdkFree: Boolean,
    val adSdkNotes: String,
    val systemBackupExcluded: Boolean,
    val exportAuditEntries: List<String>
)
