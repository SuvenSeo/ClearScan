package com.ardeno.clearscan.ui.settings

import com.ardeno.clearscan.backup.BackupPassphraseRequest
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.update.AppUpdateInfo
import com.ardeno.clearscan.vault.PrivacyStatus

data class SettingsUiState(
    val vaultEnabled: Boolean = false,
    val vaultUnlocked: Boolean = true,
    val benchmarkSummary: String? = null,
    val isBackupRunning: Boolean = false,
    val selfHostConfig: SelfHostConfig = SelfHostConfig(),
    val autoPageTurnEnabled: Boolean = false,
    val imageEnhancementEnabled: Boolean = true,
    val defaultOcrLanguage: OcrLanguage = OcrLanguage.Latin,
    val isUpdateChecking: Boolean = false,
    val isUpdateDownloading: Boolean = false,
    val pendingAppUpdate: AppUpdateInfo? = null,
    val backupPassphraseRequest: BackupPassphraseRequest? = null,
    val passphraseBackupEnabled: Boolean = false,
    val wifiOnlySelfHostUpload: Boolean = true,
    val privacyStatus: PrivacyStatus? = null
)
