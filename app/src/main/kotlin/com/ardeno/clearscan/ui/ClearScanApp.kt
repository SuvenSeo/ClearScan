package com.ardeno.clearscan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ClearScanUiState
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.matchesQuery
import com.ardeno.clearscan.ui.screens.DocumentDetailSheet
import com.ardeno.clearscan.ui.screens.LibraryScreen
import com.ardeno.clearscan.ui.screens.OnboardingScreen
import com.ardeno.clearscan.ui.screens.SettingsScreen
import com.ardeno.clearscan.ui.screens.VaultLockScreen

private enum class ClearScanTab {
    Library,
    Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearScanApp(
    state: ClearScanUiState,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSignatureTextChange: (String) -> Unit,
    onPdfPasswordChange: (String) -> Unit,
    onToggleDocumentExpanded: (ScanDocument) -> Unit,
    onShareDocument: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onRetryOcr: (ScanDocument) -> Unit,
    onMergeAllDocuments: () -> Unit,
    onSplitDocument: (ScanDocument) -> Unit,
    onRotateDocument: (ScanDocument) -> Unit,
    onSignDocument: (ScanDocument) -> Unit,
    onRedactDocument: (ScanDocument) -> Unit,
    onPasswordProtectDocument: (ScanDocument) -> Unit,
    onToggleVault: () -> Unit,
    onUnlockVault: () -> Unit,
    onLockVault: () -> Unit,
    onRunOcrBenchmark: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    onLibraryViewModeChange: (LibraryViewMode) -> Unit,
    onDismissMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(ClearScanTab.Library) }
    var selectedDocument by remember { mutableStateOf<ScanDocument?>(null) }

    val visibleDocuments = remember(state.documents, state.query) {
        state.documents.filter { it.matchesQuery(state.query) }
    }
    val hasDocuments = state.documents.isNotEmpty()
    val hasSearchResults = visibleDocuments.isNotEmpty()

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissMessage()
    }

    LaunchedEffect(state.expandedDocumentId, state.documents) {
        val expandedId = state.expandedDocumentId ?: return@LaunchedEffect
        selectedDocument = state.documents.find { it.id == expandedId }
    }

    if (!state.hasCompletedOnboarding) {
        OnboardingScreen(onComplete = onCompleteOnboarding)
        return
    }

    if (state.vaultEnabled && !state.vaultUnlocked) {
        VaultLockScreen(
            onUnlockVault = onUnlockVault,
            modifier = Modifier.padding(0.dp)
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == ClearScanTab.Library,
                    onClick = { selectedTab = ClearScanTab.Library },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Library"
                        )
                    },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = selectedTab == ClearScanTab.Settings,
                    onClick = { selectedTab = ClearScanTab.Settings },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            ClearScanTab.Library -> {
                LibraryScreen(
                    modifier = Modifier.padding(padding),
                    documents = visibleDocuments,
                    query = state.query,
                    viewMode = state.libraryViewMode,
                    isWorking = state.isSaving || state.isOcrRunning || state.isPdfToolRunning,
                    showEmptySearch = hasDocuments && !hasSearchResults && state.query.isNotBlank(),
                    onQueryChange = onQueryChange,
                    onViewModeChange = onLibraryViewModeChange,
                    onScanClick = onScanClick,
                    onDocumentClick = { document ->
                        selectedDocument = document
                        onToggleDocumentExpanded(document)
                    },
                    onDeleteDocument = onDeleteDocument,
                    onMergeAllDocuments = onMergeAllDocuments
                )
            }
            ClearScanTab.Settings -> {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    vaultEnabled = state.vaultEnabled,
                    benchmarkSummary = state.benchmarkSummary,
                    onToggleVault = onToggleVault,
                    onLockVault = onLockVault,
                    onRunOcrBenchmark = onRunOcrBenchmark
                )
            }
        }
    }

    selectedDocument?.let { document ->
        val currentDocument = state.documents.find { it.id == document.id } ?: document

        DocumentDetailSheet(
            document = currentDocument,
            signatureText = state.signatureText,
            pdfPassword = state.pdfPassword,
            onDismiss = {
                selectedDocument = null
                if (state.expandedDocumentId == document.id) {
                    onToggleDocumentExpanded(document)
                }
            },
            onShare = { onShareDocument(currentDocument) },
            onDelete = {
                selectedDocument = null
                onDeleteDocument(currentDocument)
            },
            onRetryOcr = { onRetryOcr(currentDocument) },
            onSignatureTextChange = onSignatureTextChange,
            onPdfPasswordChange = onPdfPasswordChange,
            onSplit = { onSplitDocument(currentDocument) },
            onRotate = { onRotateDocument(currentDocument) },
            onSign = { onSignDocument(currentDocument) },
            onRedact = { onRedactDocument(currentDocument) },
            onPasswordProtect = { onPasswordProtectDocument(currentDocument) }
        )
    }
}
