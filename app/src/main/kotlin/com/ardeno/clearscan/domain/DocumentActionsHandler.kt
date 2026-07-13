package com.ardeno.clearscan.domain

import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.export.SelfHostExporter
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.settings.SettingsUiState
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DocumentActionsHandler(
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val selfHostExporter: SelfHostExporter,
    private val getFolders: () -> List<DocumentFolder>,
    private val getSelectedIds: () -> Set<String>,
    private val getSettings: () -> SettingsUiState,
    private val onReplaceDocument: (ScanDocument) -> Unit,
    private val onRefreshAfterDeletion: (Set<String>, Int) -> Unit,
    private val onMessage: (String) -> Unit,
    private val onSelfHostUploadingChanged: (Boolean) -> Unit,
    private val exportPathFor: (ScanDocument) -> String?,
    private val logDocumentExport: (ScanDocument, String) -> Unit
) {
    fun updateDocumentTags(document: ScanDocument, tags: List<String>) {
        scope.launch {
            repository.updateDocumentTags(document.id, tags)?.let {
                onReplaceDocument(it)
                onMessage("Tags updated.")
            }
        }
    }

    fun toggleDocumentFavorite(document: ScanDocument) {
        scope.launch {
            repository.setDocumentFavorite(document.id, !document.isFavorite)?.let { updated ->
                onReplaceDocument(updated)
                onMessage(if (updated.isFavorite) "Added to favorites." else "Removed from favorites.")
            }
        }
    }

    fun moveDocumentToFolder(document: ScanDocument, folderId: String?) {
        scope.launch {
            repository.moveDocumentToFolder(document.id, folderId)?.let { updated ->
                onReplaceDocument(updated)
                val folderName = folderId?.let { id -> getFolders().find { it.id == id }?.name }
                onMessage(if (folderName == null) "Moved to library." else "Moved to \"$folderName\".")
            }
        }
    }

    fun deleteSelectedDocuments() {
        val selectedIds = getSelectedIds()
        if (selectedIds.isEmpty()) {
            onMessage("Select at least one document.")
            return
        }
        scope.launch {
            onRefreshAfterDeletion(selectedIds, repository.deleteDocuments(selectedIds))
        }
    }

    fun deleteDocument(document: ScanDocument) {
        scope.launch {
            if (repository.deleteDocument(document.id)) {
                onRefreshAfterDeletion(setOf(document.id), 1)
            }
        }
    }

    fun uploadToSelfHost(document: ScanDocument) {
        val settings = getSettings()
        val config = settings.selfHostConfig
        if (!config.enabled) {
            onMessage("Enable self-host export in Settings first.")
            return
        }
        if (!config.isConfigured) {
            onMessage("Add your self-host endpoint and credentials in Settings.")
            return
        }
        scope.launch {
            onSelfHostUploadingChanged(true)
            runCatching {
                val exportPath = exportPathFor(document) ?: error("No export file is available for this scan.")
                val exportFile = File(exportPath)
                require(exportFile.exists()) { "The export file is missing." }
                selfHostExporter.export(document, exportFile, config, wifiOnly = settings.wifiOnlySelfHostUpload)
            }.onSuccess {
                logDocumentExport(document, "self-host")
                onSelfHostUploadingChanged(false)
                onMessage("Uploaded ${document.title} to your self-host target.")
            }.onFailure { error ->
                onSelfHostUploadingChanged(false)
                onMessage(error.localizedMessage ?: "Self-host upload failed.")
            }
        }
    }
}
