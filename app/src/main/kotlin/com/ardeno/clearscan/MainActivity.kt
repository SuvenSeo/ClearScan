package com.ardeno.clearscan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.scanner.ScannerImport
import com.ardeno.clearscan.ui.ClearScanApp
import com.ardeno.clearscan.ui.theme.ClearScanTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : FragmentActivity() {
    private val viewModel by viewModels<ClearScanViewModel>()

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
                pageUris = pageUris
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value

            ClearScanTheme {
                ClearScanApp(
                    state = state,
                    onScanClick = ::startDocumentScanner,
                    onImportClick = ::startDocumentScanner,
                    onQueryChange = viewModel::updateQuery,
                    onSignatureTextChange = viewModel::updateSignatureText,
                    onPdfPasswordChange = viewModel::updatePdfPassword,
                    onToggleDocumentExpanded = viewModel::toggleDocumentExpanded,
                    onShareDocument = ::shareDocument,
                    onDeleteDocument = viewModel::deleteDocument,
                    onRetryOcr = viewModel::retryOcr,
                    onMergeAllDocuments = viewModel::mergeAllDocuments,
                    onSplitDocument = viewModel::splitDocument,
                    onRotateDocument = viewModel::rotateDocument,
                    onSignDocument = viewModel::signDocument,
                    onRedactDocument = viewModel::redactDocument,
                    onPasswordProtectDocument = viewModel::passwordProtectDocument,
                    onToggleVault = ::toggleVault,
                    onUnlockVault = ::unlockVault,
                    onLockVault = viewModel::lockVault,
                    onRunOcrBenchmark = viewModel::runSinhalaTamilBenchmarkSelfCheck,
                    onDismissMessage = viewModel::clearMessage
                )
            }
        }
    }

    private fun startDocumentScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(64)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
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
}
