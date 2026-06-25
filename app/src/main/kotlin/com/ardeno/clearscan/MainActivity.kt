package com.ardeno.clearscan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import com.ardeno.clearscan.capture.PageTurnCaptureActivity
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.update.ApkUpdateManager
import com.ardeno.clearscan.export.DocumentPrintHelper
import com.ardeno.clearscan.export.TextExportHelper
import com.ardeno.clearscan.widget.ScanWidgetProvider
import com.ardeno.clearscan.ui.ClearScanApp
import com.ardeno.clearscan.ui.theme.ClearScanTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : FragmentActivity() {
    private val viewModel by viewModels<ClearScanViewModel>()
    private var pendingScanMode = ScanMode.Document

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        val pdfUri = scanResult?.pdf?.uri
        val pageUris = scanResult?.pages.orEmpty().map { it.imageUri }

        if (pdfUri == null && pageUris.isEmpty()) {
            viewModel.reportMessage("No pages were returned from the scanner.")
            return@registerForActivityResult
        }

        viewModel.saveScan(
            ScannerImport(
                pdfUri = pdfUri,
                pageUris = pageUris,
                scanMode = pendingScanMode,
                enhanceImages = viewModel.uiState.value.imageEnhancementEnabled
            )
        )
    }

    private val pageTurnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        val pagePaths = activityResult.data
            ?.getStringArrayListExtra(PageTurnCaptureActivity.EXTRA_PAGE_PATHS)
            .orEmpty()

        viewModel.savePageTurnCapture(pagePaths)
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let(viewModel::exportBackup)
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::importBackup)
    }

    private val fileImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.importFiles(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value

            ClearScanTheme {
                ClearScanApp(
                    state = state,
                    onScanClick = ::startDocumentScanner,
                    onIdScanClick = ::startIdCardScanner,
                    onImportClick = ::startFileImport,
                    onQueryChange = viewModel::updateQuery,
                    onSignatureTextChange = viewModel::updateSignatureText,
                    onPdfPasswordChange = viewModel::updatePdfPassword,
                    onToggleDocumentExpanded = viewModel::toggleDocumentExpanded,
                    onShareDocument = ::shareDocument,
                    onExportText = ::exportText,
                    onPrintDocument = ::printDocument,
                    onDeleteDocument = viewModel::deleteDocument,
                    onRetryOcr = viewModel::retryOcr,
                    onDocumentOcrLanguageChange = viewModel::setDocumentOcrLanguage,
                    onMergeAllDocuments = viewModel::mergeAllDocuments,
                    onSplitDocument = viewModel::splitDocument,
                    onRotateDocument = viewModel::rotateDocument,
                    onSignDocument = viewModel::signDocument,
                    onRedactDocument = viewModel::redactDocument,
                    onApplyAnnotations = viewModel::applyAnnotations,
                    onPasswordProtectDocument = viewModel::passwordProtectDocument,
                    onReorderDocument = viewModel::reorderDocument,
                    onDeletePagesFromDocument = viewModel::deletePagesFromDocument,
                    onCompressDocument = viewModel::compressDocument,
                    onCompressQualityChange = viewModel::updateCompressQuality,
                    onSelectFolder = viewModel::setSelectedFolder,
                    onSelectFavorites = { viewModel.setShowFavoritesOnly(true) },
                    onCreateFolder = viewModel::createFolder,
                    onEnterSelectionMode = viewModel::enterSelectionMode,
                    onExitSelectionMode = viewModel::exitSelectionMode,
                    onToggleDocumentSelection = viewModel::toggleDocumentSelection,
                    onSelectAllVisible = viewModel::selectAllVisibleDocuments,
                    onMergeSelected = viewModel::mergeSelectedDocuments,
                    onExportSelected = ::shareSelectedDocuments,
                    onDeleteSelected = viewModel::deleteSelectedDocuments,
                    onToggleFavorite = viewModel::toggleDocumentFavorite,
                    onUpdateTags = viewModel::updateDocumentTags,
                    onMoveToFolder = viewModel::moveDocumentToFolder,
                    onToggleVault = ::toggleVault,
                    onUnlockVault = ::unlockVault,
                    onLockVault = viewModel::lockVault,
                    onRunOcrBenchmark = viewModel::runSinhalaTamilBenchmarkSelfCheck,
                    onOpenPrivacyDashboard = viewModel::refreshPrivacyStatus,
                    onClosePrivacyDashboard = viewModel::refreshPrivacyStatus,
                    onExportBackup = ::startBackupExport,
                    onImportBackup = ::startBackupImport,
                    onSelfHostConfigChange = viewModel::updateSelfHostConfig,
                    onSaveSelfHostConfig = viewModel::saveSelfHostConfig,
                    onUploadToSelfHost = viewModel::uploadToSelfHost,
                    onRedactIdFields = viewModel::redactIdSensitiveFields,
                    onAutoPageTurnChange = viewModel::setAutoPageTurnEnabled,
                    onImageEnhancementChange = viewModel::setImageEnhancementEnabled,
                    onDefaultOcrLanguageChange = viewModel::setDefaultOcrLanguage,
                    onCompleteOnboarding = viewModel::completeOnboarding,
                    onLibraryViewModeChange = viewModel::setLibraryViewMode,
                    onDismissMessage = viewModel::clearMessage,
                    onCheckForAppUpdate = viewModel::checkForAppUpdate,
                    onDismissAppUpdate = viewModel::dismissAppUpdate,
                    onDownloadAppUpdate = ::startAppUpdateDownload
                )
            }
        }
    }

    private fun startFileImport() {
        fileImportLauncher.launch(arrayOf("application/pdf", "image/*"))
    }

    private fun startDocumentScanner() {
        pendingScanMode = ScanMode.Document
        if (viewModel.uiState.value.autoPageTurnEnabled) {
            pageTurnLauncher.launch(Intent(this, PageTurnCaptureActivity::class.java))
            return
        }
        launchScanner(
            pageLimit = 64,
            scannerMode = GmsDocumentScannerOptions.SCANNER_MODE_FULL
        )
    }

    private fun startIdCardScanner() {
        pendingScanMode = ScanMode.IdCard
        launchScanner(
            pageLimit = 2,
            scannerMode = GmsDocumentScannerOptions.SCANNER_MODE_BASE
        )
    }

    private fun launchScanner(pageLimit: Int, scannerMode: Int) {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(pageLimit)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(scannerMode)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { error ->
                val message = error.localizedMessage ?: "Document scanner is unavailable on this device."
                viewModel.reportMessage(message)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun shareDocument(document: ScanDocument) {
        val exportPath = viewModel.exportPathFor(document)
        if (exportPath == null) {
            viewModel.reportMessage("No export file is available for this scan.")
            return
        }

        val file = File(exportPath)
        if (!file.exists()) {
            viewModel.reportMessage("The export file is missing.")
            return
        }

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = viewModel.exportMimeTypeFor(document)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share scan"))
        viewModel.logDocumentExport(document)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            ScanWidgetProvider.ACTION_SCAN,
            "com.ardeno.clearscan.action.SCAN" -> {
                if (viewModel.uiState.value.hasCompletedOnboarding) {
                    startDocumentScanner()
                }
                intent.action = null
            }
            "com.ardeno.clearscan.action.SCAN_ID" -> {
                if (viewModel.uiState.value.hasCompletedOnboarding) {
                    startIdCardScanner()
                }
                intent.action = null
            }
            "com.ardeno.clearscan.action.IMPORT" -> {
                if (viewModel.uiState.value.hasCompletedOnboarding) {
                    startFileImport()
                }
                intent.action = null
            }
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    viewModel.importFiles(listOf(uri))
                }
                intent.action = null
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                if (uris.isNotEmpty()) {
                    viewModel.importFiles(uris)
                }
                intent.action = null
            }
        }
    }

    private fun exportText(document: ScanDocument) {
        val shareIntent = TextExportHelper.createShareIntent(this, document)
        if (shareIntent == null) {
            viewModel.reportMessage("No OCR text is available to export yet.")
            return
        }
        startActivity(Intent.createChooser(shareIntent, "Export OCR text"))
        viewModel.logDocumentExport(document, exportKind = "text")
    }

    private fun printDocument(document: ScanDocument) {
        val started = DocumentPrintHelper.printDocument(this, document)
        if (!started) {
            viewModel.reportMessage("No PDF is available to print for this scan.")
            return
        }
        viewModel.logDocumentExport(document, exportKind = "print")
    }

    private fun startBackupExport() {
        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        exportBackupLauncher.launch("clearscan-backup-$timestamp.csbak")
    }

    private fun startBackupImport() {
        importBackupLauncher.launch(arrayOf("application/octet-stream", "*/*"))
    }

    private fun shareSelectedDocuments() {
        val exports = viewModel.exportPathsForSelectedDocuments()
        if (exports.isEmpty()) {
            viewModel.reportMessage("No export files are available for the selected scans.")
            return
        }

        val uris = ArrayList<android.net.Uri>()
        var mimeType = exports.first().second

        exports.forEach { (path, type) ->
            val file = File(path)
            if (!file.exists()) return@forEach
            uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
            if (mimeType != type) {
                mimeType = "*/*"
            }
        }

        if (uris.isEmpty()) {
            viewModel.reportMessage("The selected export files are missing.")
            return
        }

        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                this.type = exports.first().second
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                this.type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(shareIntent, "Share selected scans"))
        val selectedIds = viewModel.uiState.value.selectedDocumentIds
        viewModel.uiState.value.documents
            .filter { it.id in selectedIds }
            .forEach { document -> viewModel.logDocumentExport(document, exportKind = "bulk-share") }
        viewModel.exitSelectionMode()
    }

    private fun toggleVault() {
        val state = viewModel.uiState.value
        if (state.vaultEnabled) {
            viewModel.setVaultEnabled(false)
            return
        }

        authenticateForVault(
            title = "Enable ClearScan vault",
            subtitle = "Confirm your identity before enabling vault lock.",
            onSuccess = { viewModel.setVaultEnabled(true) }
        )
    }

    private fun unlockVault() {
        authenticateForVault(
            title = "Unlock ClearScan",
            subtitle = "Use biometrics or device credentials to unlock your scans.",
            onSuccess = viewModel::unlockVault
        )
    }

    private fun authenticateForVault(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.reportMessage("Set up a screen lock or biometric first.")
                return
            }
            else -> {
                viewModel.reportMessage("Biometric vault is unavailable on this device.")
                return
            }
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    viewModel.reportMessage(errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun startAppUpdateDownload() {
        val updateManager = ApkUpdateManager(this)
        if (!updateManager.canInstallPackages()) {
            startActivity(updateManager.createInstallPermissionIntent())
            viewModel.reportMessage("Allow ClearScan to install updates, then tap Download again.")
            return
        }
        viewModel.downloadPendingAppUpdate()
    }
}
