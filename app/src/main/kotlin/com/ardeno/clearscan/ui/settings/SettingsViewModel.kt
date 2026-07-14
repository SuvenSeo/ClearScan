package com.ardeno.clearscan.ui.settings

import android.app.Application
import android.net.Uri
import com.ardeno.clearscan.backup.BackupPassphraseAction
import com.ardeno.clearscan.backup.BackupPassphraseRequest
import com.ardeno.clearscan.backup.BackupRestoreManager
import com.ardeno.clearscan.data.AppPreferences
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.data.SelfHostSettings
import com.ardeno.clearscan.duplicate.DuplicateDetector
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.OcrBenchmark
import com.ardeno.clearscan.ocr.OcrBenchmarkRunner
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.update.AppUpdateCheckResult
import com.ardeno.clearscan.update.ApkUpdateManager
import com.ardeno.clearscan.vault.ExportAuditLog
import com.ardeno.clearscan.vault.PrivacyStatusProvider
import com.ardeno.clearscan.vault.VaultCrypto
import com.ardeno.clearscan.vault.VaultKeyMigration
import com.ardeno.clearscan.vault.VaultSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupImportReload(
    val documents: List<ScanDocument>,
    val folders: List<DocumentFolder>,
    val duplicateDocumentIds: Set<String>
)

class SettingsViewModel(
    private val application: Application,
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSettings: VaultSettings,
    private val vaultKeyMigration: VaultKeyMigration,
    private val exportAuditLog: ExportAuditLog,
    private val privacyStatusProvider: PrivacyStatusProvider,
    private val backupRestoreManager: BackupRestoreManager,
    private val appPreferences: AppPreferences,
    private val selfHostSettings: SelfHostSettings,
    private val apkUpdateManager: ApkUpdateManager,
    private val duplicateDetector: DuplicateDetector,
    private val onMessage: (String) -> Unit,
    private val onBackupImportSuccess: (BackupImportReload) -> Unit
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { current ->
            current.copy(
                selfHostConfig = selfHostSettings.load(),
                autoPageTurnEnabled = appPreferences.autoPageTurnEnabled,
                imageEnhancementEnabled = appPreferences.imageEnhancementEnabled,
                defaultOcrLanguage = appPreferences.defaultOcrLanguage,
                passphraseBackupEnabled = appPreferences.passphraseBackupEnabled,
                wifiOnlySelfHostUpload = appPreferences.wifiOnlySelfHostUpload
            )
        }
    }

    fun initializeVaultState() {
        scope.launch {
            runCatching { vaultCrypto.ensureVaultKey() }
            val vaultEnabled = vaultSettings.isEnabled
            _uiState.update { current ->
                current.copy(
                    vaultEnabled = vaultEnabled,
                    vaultUnlocked = !vaultEnabled,
                    privacyStatus = privacyStatusProvider.load()
                )
            }
        }
    }

    fun setAutoPageTurnEnabled(enabled: Boolean) {
        appPreferences.setAutoPageTurnEnabled(enabled)
        _uiState.update { it.copy(autoPageTurnEnabled = enabled) }
    }

    fun setImageEnhancementEnabled(enabled: Boolean) {
        appPreferences.setImageEnhancementEnabled(enabled)
        _uiState.update { it.copy(imageEnhancementEnabled = enabled) }
    }

    fun setDefaultOcrLanguage(language: OcrLanguage) {
        appPreferences.setDefaultOcrLanguage(language)
        _uiState.update { it.copy(defaultOcrLanguage = language) }
    }

    fun setPassphraseBackupEnabled(enabled: Boolean) {
        appPreferences.setPassphraseBackupEnabled(enabled)
        _uiState.update { it.copy(passphraseBackupEnabled = enabled) }
    }

    fun setWifiOnlySelfHostUpload(enabled: Boolean) {
        appPreferences.setWifiOnlySelfHostUpload(enabled)
        _uiState.update { it.copy(wifiOnlySelfHostUpload = enabled) }
    }

    fun setVaultEnabled(enabled: Boolean) {
        scope.launch {
            runCatching {
                if (enabled) {
                    vaultCrypto.ensureBiometricVaultKey()
                } else {
                    vaultCrypto.ensureVaultKey()
                }
                vaultCrypto.healthCheck()
            }.onSuccess { healthy ->
                vaultSettings.setEnabled(enabled)
                if (enabled) {
                    vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_BIOMETRIC)
                } else {
                    vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_NONE)
                }
                _uiState.update { current ->
                    current.copy(
                        vaultEnabled = enabled,
                        vaultUnlocked = !enabled
                    )
                }
                onMessage(
                    when {
                        enabled && healthy -> "Vault enabled. Biometric unlock will be required."
                        enabled -> "Vault enabled."
                        else -> "Vault disabled."
                    }
                )
            }.onFailure { error ->
                onMessage(error.localizedMessage ?: "Vault setup failed.")
            }
        }
    }

    fun unlockVault() {
        // Unlock is deferred to MainActivity biometric CryptoObject flow.
    }

    fun onVaultCryptoUnlocked() {
        vaultCrypto.markSessionAuthorized()
        _uiState.update { it.copy(vaultUnlocked = true, vaultAuthError = false) }
        onMessage("Vault unlocked.")
        scope.launch {
            runCatching { vaultKeyMigration.migrateIfNeeded() }
                .onFailure { error ->
                    onMessage(error.localizedMessage ?: "Vault key migration failed.")
                }
        }
    }

    fun reportVaultAuthError() {
        _uiState.update { it.copy(vaultAuthError = true) }
    }

    fun clearVaultAuthError() {
        _uiState.update { it.copy(vaultAuthError = false) }
    }

    fun lockVault(announce: Boolean = true) {
        vaultCrypto.clearSession()
        _uiState.update { current ->
            if (!current.vaultEnabled) {
                current
            } else {
                current.copy(vaultUnlocked = false, vaultAuthError = false)
            }
        }
        if (announce) {
            onMessage("Vault locked.")
        }
        scope.launch {
            repository.clearReadableCache()
        }
    }

    fun updateSelfHostConfig(config: SelfHostConfig) {
        _uiState.update { it.copy(selfHostConfig = config) }
    }

    fun saveSelfHostConfig() {
        val config = _uiState.value.selfHostConfig
        selfHostSettings.save(config)
        _uiState.update {
            it.copy(
                selfHostConfig = selfHostSettings.load()
            )
        }
        onMessage(
            if (config.enabled) {
                "Self-host export enabled. Uploads require your explicit action."
            } else {
                "Self-host export settings saved."
            }
        )
    }

    fun runSinhalaTamilBenchmarkSelfCheck() {
        scope.launch {
            onMessage("Running on-device Sinhala/Tamil OCR benchmark…")
            runCatching {
                OcrBenchmarkRunner.runSyntheticEngineBenchmark(application)
            }.onSuccess { metrics ->
                val summary = buildString {
                    append(OcrBenchmark.summary(metrics))
                    append("\nEngine: Tesseract 5 (LSTM) · synthetic print samples")
                }
                _uiState.update { current ->
                    current.copy(benchmarkSummary = summary)
                }
                onMessage("Sinhala/Tamil OCR benchmark finished.")
            }.onFailure { error ->
                onMessage(error.localizedMessage ?: "OCR benchmark failed.")
            }
        }
    }

    fun refreshPrivacyStatus() {
        _uiState.update { it.copy(privacyStatus = privacyStatusProvider.load()) }
    }

    fun logDocumentExport(documentId: String, documentTitle: String, exportKind: String) {
        exportAuditLog.record(
            documentId = documentId,
            documentTitle = documentTitle,
            exportKind = exportKind
        )
        refreshPrivacyStatus()
    }

    fun onBackupExportUriSelected(targetUri: Uri) {
        if (appPreferences.passphraseBackupEnabled) {
            _uiState.update {
                it.copy(
                    backupPassphraseRequest = BackupPassphraseRequest(
                        action = BackupPassphraseAction.Export,
                        uri = targetUri,
                        confirmPassphrase = true
                    )
                )
            }
        } else {
            exportBackupWithPassphrase(targetUri, null)
        }
    }

    fun onBackupImportUriSelected(sourceUri: Uri) {
        val version = backupRestoreManager.backupVersion(sourceUri)
        if (version == BackupRestoreManager.BACKUP_VERSION_PASSPHRASE) {
            _uiState.update {
                it.copy(
                    backupPassphraseRequest = BackupPassphraseRequest(
                        action = BackupPassphraseAction.Import,
                        uri = sourceUri
                    )
                )
            }
        } else {
            importBackupWithPassphrase(sourceUri, null)
        }
    }

    fun submitBackupPassphrase(passphrase: CharArray, confirmation: CharArray?) {
        val request = _uiState.value.backupPassphraseRequest ?: return
        if (request.confirmPassphrase && confirmation != null && !passphrase.contentEquals(confirmation)) {
            onMessage("Passphrases do not match.")
            return
        }
        _uiState.update { it.copy(backupPassphraseRequest = null) }
        when (request.action) {
            BackupPassphraseAction.Export -> exportBackupWithPassphrase(request.uri, passphrase)
            BackupPassphraseAction.Import -> importBackupWithPassphrase(request.uri, passphrase)
        }
    }

    fun dismissBackupPassphrase() {
        _uiState.update { it.copy(backupPassphraseRequest = null) }
    }

    fun checkForAppUpdate() {
        if (_uiState.value.isUpdateChecking) return

        scope.launch {
            _uiState.update { it.copy(isUpdateChecking = true) }
            val result = apkUpdateManager.checkForUpdate()
            _uiState.update { it.copy(isUpdateChecking = false) }

            result.onSuccess { checkResult ->
                when (checkResult) {
                    is AppUpdateCheckResult.Available -> {
                        _uiState.update { current ->
                            current.copy(pendingAppUpdate = checkResult.info)
                        }
                    }
                    is AppUpdateCheckResult.UpToDate -> {
                        onMessage("ClearScan is up to date (${checkResult.latestVersionName}).")
                    }
                    is AppUpdateCheckResult.Unsupported -> {
                        val requiredUpdate = checkResult.info
                        onMessage(
                            "This build is too old for ${requiredUpdate.versionName}. Install the latest APK manually from GitHub Releases."
                        )
                    }
                }
            }.onFailure { error ->
                onMessage(error.localizedMessage ?: "Could not check for updates.")
            }
        }
    }

    fun dismissAppUpdate() {
        _uiState.update { it.copy(pendingAppUpdate = null, isUpdateDownloading = false) }
    }

    fun downloadPendingAppUpdate() {
        val update = _uiState.value.pendingAppUpdate ?: return
        if (_uiState.value.isUpdateDownloading) return

        _uiState.update { it.copy(isUpdateDownloading = true) }
        apkUpdateManager.enqueueDownload(update)
        onMessage("Downloading ClearScan ${update.versionName}…")
    }

    private fun exportBackupWithPassphrase(targetUri: Uri, passphrase: CharArray?) {
        scope.launch {
            _uiState.update { it.copy(isBackupRunning = true) }
            val result = backupRestoreManager.exportBackup(targetUri, passphrase)
            _uiState.update { current ->
                current.copy(
                    isBackupRunning = false,
                    privacyStatus = privacyStatusProvider.load()
                )
            }
            onMessage(result.message)
        }
    }

    private fun importBackupWithPassphrase(sourceUri: Uri, passphrase: CharArray?) {
        scope.launch {
            _uiState.update { it.copy(isBackupRunning = true) }
            val result = backupRestoreManager.importBackup(sourceUri, passphrase)
            if (result.requiresPassphrase) {
                _uiState.update {
                    it.copy(
                        isBackupRunning = false,
                        backupPassphraseRequest = BackupPassphraseRequest(
                            action = BackupPassphraseAction.Import,
                            uri = sourceUri
                        )
                    )
                }
                return@launch
            }
            if (result.success) {
                val documents = repository.loadDocuments()
                val folders = repository.loadFolders()
                val duplicateIds = duplicateDetector.duplicateDocumentIds(documents)
                onBackupImportSuccess(
                    BackupImportReload(
                        documents = documents,
                        folders = folders,
                        duplicateDocumentIds = duplicateIds
                    )
                )
            }
            _uiState.update { current ->
                current.copy(
                    isBackupRunning = false,
                    privacyStatus = privacyStatusProvider.load()
                )
            }
            onMessage(result.message)
        }
    }
}
