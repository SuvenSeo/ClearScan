package com.ardeno.clearscan

import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ocr.IdRedactionSuggestion
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.ui.settings.SettingsUiState

data class ClearScanUiState(
    val documents: List<ScanDocument> = emptyList(),
    val folders: List<DocumentFolder> = emptyList(),
    val selectedFolderId: String? = null,
    val showFavoritesOnly: Boolean = false,
    val showTrashOnly: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedDocumentIds: Set<String> = emptySet(),
    val duplicateDocumentIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val isOcrRunning: Boolean = false,
    val isPdfToolRunning: Boolean = false,
    val query: String = "",
    val signatureText: String = "",
    val pdfPassword: String = "",
    val compressQuality: PdfCompressQuality = PdfCompressQuality.Balanced,
    val expandedDocumentId: String? = null,
    val settings: SettingsUiState = SettingsUiState(),
    val message: String? = null,
    val hasCompletedOnboarding: Boolean = false,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.List,
    val isSelfHostUploading: Boolean = false,
    val idRedactionSuggestions: Map<String, IdRedactionSuggestion> = emptyMap()
)
