package com.ardeno.clearscan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.matchesQuery
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.ui.screens.DocumentAnnotatorScreen
import com.ardeno.clearscan.ui.screens.DocumentDetailSheet
import com.ardeno.clearscan.ui.screens.LibraryScreen
import com.ardeno.clearscan.ui.screens.OnboardingScreen
import com.ardeno.clearscan.ui.screens.PageEditorMode
import com.ardeno.clearscan.ui.screens.PageEditorSheet
import com.ardeno.clearscan.ui.screens.PrivacyDashboardScreen
import com.ardeno.clearscan.ui.screens.SettingsScreen
import com.ardeno.clearscan.ui.components.AppUpdateDialog
import com.ardeno.clearscan.ui.screens.VaultLockScreen
import com.ardeno.clearscan.ui.theme.ClearScanElevation
import com.ardeno.clearscan.ui.theme.ClearScanMotion

private enum class ClearScanTab {
    Library,
    Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearScanApp(
    state: ClearScanUiState,
    onScanClick: () -> Unit,
    onIdScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSignatureTextChange: (String) -> Unit,
    onPdfPasswordChange: (String) -> Unit,
    onToggleDocumentExpanded: (ScanDocument) -> Unit,
    onShareDocument: (ScanDocument) -> Unit,
    onExportText: (ScanDocument) -> Unit,
    onPrintDocument: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onRetryOcr: (ScanDocument) -> Unit,
    onDocumentOcrLanguageChange: (ScanDocument, OcrLanguage) -> Unit,
    onMergeAllDocuments: () -> Unit,
    onSplitDocument: (ScanDocument) -> Unit,
    onRotateDocument: (ScanDocument) -> Unit,
    onSignDocument: (ScanDocument) -> Unit,
    onRedactDocument: (ScanDocument) -> Unit,
    onApplyAnnotations: (ScanDocument, Map<Int, List<PageAnnotation>>) -> Unit,
    onPasswordProtectDocument: (ScanDocument) -> Unit,
    onReorderDocument: (ScanDocument, List<Int>) -> Unit,
    onDeletePagesFromDocument: (ScanDocument, List<Int>) -> Unit,
    onCompressDocument: (ScanDocument) -> Unit,
    onCompressQualityChange: (PdfCompressQuality) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onSelectFavorites: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleDocumentSelection: (String) -> Unit,
    onSelectAllVisible: (List<String>) -> Unit,
    onMergeSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onToggleFavorite: (ScanDocument) -> Unit,
    onUpdateTags: (ScanDocument, List<String>) -> Unit,
    onMoveToFolder: (ScanDocument, String?) -> Unit,
    onToggleVault: () -> Unit,
    onUnlockVault: () -> Unit,
    onLockVault: () -> Unit,
    onRunOcrBenchmark: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onClosePrivacyDashboard: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onSelfHostConfigChange: (com.ardeno.clearscan.data.SelfHostConfig) -> Unit,
    onSaveSelfHostConfig: () -> Unit,
    onUploadToSelfHost: (ScanDocument) -> Unit,
    onRedactIdFields: (ScanDocument) -> Unit,
    onAutoPageTurnChange: (Boolean) -> Unit,
    onImageEnhancementChange: (Boolean) -> Unit,
    onDefaultOcrLanguageChange: (OcrLanguage) -> Unit,
    onCompleteOnboarding: () -> Unit,
    onLibraryViewModeChange: (LibraryViewMode) -> Unit,
    onDismissMessage: () -> Unit,
    onCheckForAppUpdate: () -> Unit,
    onDismissAppUpdate: () -> Unit,
    onDownloadAppUpdate: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(ClearScanTab.Library) }
    var selectedDocument by remember { mutableStateOf<ScanDocument?>(null) }
    var annotatingDocument by remember { mutableStateOf<ScanDocument?>(null) }
    var pageEditorTarget by remember { mutableStateOf<ScanDocument?>(null) }
    var pageEditorMode by remember { mutableStateOf<PageEditorMode?>(null) }
    var showPrivacyDashboard by rememberSaveable { mutableStateOf(false) }

    val visibleDocuments = remember(
        state.documents,
        state.query,
        state.selectedFolderId,
        state.showFavoritesOnly
    ) {
        state.documents
            .filter { document ->
                when {
                    state.showFavoritesOnly -> document.isFavorite
                    state.selectedFolderId != null -> document.folderId == state.selectedFolderId
                    else -> true
                }
            }
            .filter { it.matchesQuery(state.query) }
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

    if (showPrivacyDashboard && state.privacyStatus != null) {
        PrivacyDashboardScreen(
            status = state.privacyStatus,
            onBack = {
                showPrivacyDashboard = false
                onClosePrivacyDashboard()
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = ClearScanElevation.navBar
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
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(ClearScanMotion.fadeMedium) togetherWith fadeOut(ClearScanMotion.fadeFast)
            },
            label = "tabContent"
        ) { tab ->
            when (tab) {
            ClearScanTab.Library -> {
                LibraryScreen(
                    modifier = Modifier.padding(padding),
                    documents = visibleDocuments,
                    folders = state.folders,
                    selectedFolderId = state.selectedFolderId,
                    showFavoritesOnly = state.showFavoritesOnly,
                    selectionMode = state.selectionMode,
                    selectedDocumentIds = state.selectedDocumentIds,
                    duplicateDocumentIds = state.duplicateDocumentIds,
                    query = state.query,
                    viewMode = state.libraryViewMode,
                    isWorking = state.isSaving || state.isOcrRunning || state.isPdfToolRunning,
                    showEmptySearch = hasDocuments && !hasSearchResults && state.query.isNotBlank(),
                    onQueryChange = onQueryChange,
                    onViewModeChange = onLibraryViewModeChange,
                    onSelectAllDocuments = { onSelectFolder(null) },
                    onSelectFolder = onSelectFolder,
                    onSelectFavorites = onSelectFavorites,
                    onCreateFolder = onCreateFolder,
                    onEnterSelectionMode = onEnterSelectionMode,
                    onExitSelectionMode = onExitSelectionMode,
                    onToggleDocumentSelection = onToggleDocumentSelection,
                    onSelectAllVisible = {
                        onSelectAllVisible(visibleDocuments.map { it.id })
                    },
                    onMergeSelected = onMergeSelected,
                    onExportSelected = onExportSelected,
                    onDeleteSelected = onDeleteSelected,
                    onScanClick = onScanClick,
                    onIdScanClick = onIdScanClick,
                    onImportClick = onImportClick,
                    onDocumentClick = { document ->
                        if (state.selectionMode) {
                            onToggleDocumentSelection(document.id)
                        } else {
                            selectedDocument = document
                            onToggleDocumentExpanded(document)
                        }
                    },
                    onDeleteDocument = onDeleteDocument
                )
            }
            ClearScanTab.Settings -> {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    vaultEnabled = state.vaultEnabled,
                    benchmarkSummary = state.benchmarkSummary,
                    isBackupRunning = state.isBackupRunning,
                    autoPageTurnEnabled = state.autoPageTurnEnabled,
                    imageEnhancementEnabled = state.imageEnhancementEnabled,
                    defaultOcrLanguage = state.defaultOcrLanguage,
                    selfHostConfig = state.selfHostConfig,
                    onSelfHostConfigChange = onSelfHostConfigChange,
                    onSaveSelfHostConfig = onSaveSelfHostConfig,
                    onToggleVault = onToggleVault,
                    onLockVault = onLockVault,
                    onRunOcrBenchmark = onRunOcrBenchmark,
                    onOpenPrivacyDashboard = {
                        showPrivacyDashboard = true
                        onOpenPrivacyDashboard()
                    },
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onAutoPageTurnChange = onAutoPageTurnChange,
                    onImageEnhancementChange = onImageEnhancementChange,
                    onDefaultOcrLanguageChange = onDefaultOcrLanguageChange,
                    isUpdateChecking = state.isUpdateChecking,
                    onCheckForAppUpdate = onCheckForAppUpdate
                )
            }
            }
        }
    }

    selectedDocument?.let { document ->
        val currentDocument = state.documents.find { it.id == document.id } ?: document

        DocumentDetailSheet(
            document = currentDocument,
            folders = state.folders,
            isDuplicate = currentDocument.id in state.duplicateDocumentIds,
            signatureText = state.signatureText,
            pdfPassword = state.pdfPassword,
            compressQuality = state.compressQuality,
            selfHostEnabled = state.selfHostConfig.enabled && state.selfHostConfig.isConfigured,
            isSelfHostUploading = state.isSelfHostUploading,
            idRedactionSuggestion = state.idRedactionSuggestions[currentDocument.id],
            onDismiss = {
                selectedDocument = null
                if (state.expandedDocumentId == document.id) {
                    onToggleDocumentExpanded(document)
                }
            },
            onShare = { onShareDocument(currentDocument) },
            onExportText = { onExportText(currentDocument) },
            onPrint = { onPrintDocument(currentDocument) },
            onUploadToSelfHost = { onUploadToSelfHost(currentDocument) },
            onRedactIdFields = { onRedactIdFields(currentDocument) },
            onDelete = {
                selectedDocument = null
                onDeleteDocument(currentDocument)
            },
            onRetryOcr = { onRetryOcr(currentDocument) },
            onOcrLanguageChange = { language ->
                onDocumentOcrLanguageChange(currentDocument, language)
            },
            onToggleFavorite = { onToggleFavorite(currentDocument) },
            onUpdateTags = { tags -> onUpdateTags(currentDocument, tags) },
            onMoveToFolder = { folderId -> onMoveToFolder(currentDocument, folderId) },
            onSignatureTextChange = onSignatureTextChange,
            onPdfPasswordChange = onPdfPasswordChange,
            onCompressQualityChange = onCompressQualityChange,
            onSplit = { onSplitDocument(currentDocument) },
            onRotate = { onRotateDocument(currentDocument) },
            onSign = { onSignDocument(currentDocument) },
            onRedact = { onRedactDocument(currentDocument) },
            onAnnotate = {
                selectedDocument = null
                annotatingDocument = currentDocument
            },
            onReorderPages = {
                pageEditorTarget = currentDocument
                pageEditorMode = PageEditorMode.Reorder
            },
            onDeletePages = {
                pageEditorTarget = currentDocument
                pageEditorMode = PageEditorMode.Delete
            },
            onCompress = { onCompressDocument(currentDocument) },
            onPasswordProtect = { onPasswordProtectDocument(currentDocument) }
        )
    }

    pageEditorTarget?.let { document ->
        val currentDocument = state.documents.find { it.id == document.id } ?: document
        val mode = pageEditorMode ?: return@let

        PageEditorSheet(
            document = currentDocument,
            mode = mode,
            onDismiss = {
                pageEditorTarget = null
                pageEditorMode = null
            },
            onConfirmReorder = { pageOrder ->
                onReorderDocument(currentDocument, pageOrder)
                pageEditorTarget = null
                pageEditorMode = null
                selectedDocument = null
            },
            onConfirmDelete = { pageIndicesToKeep ->
                onDeletePagesFromDocument(currentDocument, pageIndicesToKeep)
                pageEditorTarget = null
                pageEditorMode = null
                selectedDocument = null
            }
        )
    }

    annotatingDocument?.let { document ->
        val currentDocument = state.documents.find { it.id == document.id } ?: document

        DocumentAnnotatorScreen(
            document = currentDocument,
            isApplying = state.isPdfToolRunning,
            onDismiss = { annotatingDocument = null },
            onApply = { annotations ->
                onApplyAnnotations(currentDocument, annotations)
                annotatingDocument = null
            }
        )
    }

    state.pendingAppUpdate?.let { update ->
        AppUpdateDialog(
            update = update,
            isDownloading = state.isUpdateDownloading,
            onDismiss = onDismissAppUpdate,
            onDownload = onDownloadAppUpdate
        )
    }
}
