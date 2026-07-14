package com.ardeno.clearscan.ui.library

import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.duplicate.DuplicateDetector
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.settings.BackupImportReload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val documents: List<ScanDocument> = emptyList(),
    val folders: List<DocumentFolder> = emptyList(),
    val selectedFolderId: String? = null,
    val showFavoritesOnly: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedDocumentIds: Set<String> = emptySet(),
    val duplicateDocumentIds: Set<String> = emptySet(),
    val expandedDocumentId: String? = null,
    val query: String = ""
)

class LibraryViewModel(
    private val scope: CoroutineScope,
    private val repository: LocalDocumentRepository,
    private val duplicateDetector: DuplicateDetector,
    private val onMessage: (String) -> Unit
) {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadInitial(onComplete: (List<ScanDocument>) -> Unit = {}) {
        scope.launch {
            val documents = repository.loadDocuments()
            val folders = repository.loadFolders()
            _uiState.update {
                it.copy(
                    documents = documents,
                    folders = folders,
                    duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(documents)
                )
            }
            onComplete(documents)
        }
    }

    fun setSelectedFolder(folderId: String?) {
        _uiState.update { it.copy(selectedFolderId = folderId, showFavoritesOnly = false) }
    }

    fun setShowFavoritesOnly(showFavoritesOnly: Boolean) {
        _uiState.update {
            it.copy(showFavoritesOnly = showFavoritesOnly, selectedFolderId = if (showFavoritesOnly) null else it.selectedFolderId)
        }
    }

    fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedDocumentIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedDocumentIds = emptySet()) }
    }

    fun toggleDocumentSelection(documentId: String) {
        _uiState.update { current ->
            current.copy(
                selectedDocumentIds = if (documentId in current.selectedDocumentIds) {
                    current.selectedDocumentIds - documentId
                } else {
                    current.selectedDocumentIds + documentId
                }
            )
        }
    }

    fun selectAllVisibleDocuments(visibleDocumentIds: List<String>) {
        _uiState.update { it.copy(selectedDocumentIds = visibleDocumentIds.toSet()) }
    }

    fun createFolder(name: String) {
        scope.launch {
            runCatching { repository.createFolder(name) }
                .onSuccess { folder ->
                    _uiState.update { it.copy(folders = listOf(folder) + it.folders) }
                    onMessage("Created folder \"${folder.name}\".")
                }
                .onFailure { onMessage(it.localizedMessage ?: "Could not create folder.") }
        }
    }

    fun renameFolder(folderId: String, name: String) {
        scope.launch {
            runCatching { repository.renameFolder(folderId, name) }
                .onSuccess { folder ->
                    folder?.let {
                        _uiState.update { current ->
                            current.copy(folders = current.folders.map { existing -> if (existing.id == it.id) it else existing })
                        }
                        onMessage("Renamed folder to \"${it.name}\".")
                    }
                }
                .onFailure { onMessage(it.localizedMessage ?: "Could not rename folder.") }
        }
    }

    fun deleteFolder(folderId: String) {
        scope.launch {
            if (repository.deleteFolder(folderId)) {
                _uiState.update { current ->
                    current.copy(
                        folders = current.folders.filterNot { it.id == folderId },
                        documents = current.documents.map { if (it.folderId == folderId) it.copy(folderId = null) else it },
                        selectedFolderId = current.selectedFolderId.takeUnless { it == folderId }
                    )
                }
                onMessage("Folder deleted.")
            } else {
                onMessage("Could not delete folder.")
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun toggleDocumentExpanded(document: ScanDocument) {
        _uiState.update {
            it.copy(expandedDocumentId = if (it.expandedDocumentId == document.id) null else document.id)
        }
    }

    fun replaceDocument(document: ScanDocument) {
        _uiState.update { current ->
            val nextDocuments = current.documents.map { if (it.id == document.id) document else it }
            current.copy(documents = nextDocuments, duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(nextDocuments))
        }
    }

    fun addDocuments(documents: List<ScanDocument>, expandedDocumentId: String?) {
        _uiState.update { current ->
            val nextDocuments = documents + current.documents
            current.copy(
                documents = nextDocuments,
                duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(nextDocuments),
                expandedDocumentId = expandedDocumentId ?: current.expandedDocumentId
            )
        }
    }

    fun setDocuments(documents: List<ScanDocument>, expandedDocumentId: String?) {
        _uiState.update {
            it.copy(
                documents = documents,
                duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(documents),
                expandedDocumentId = expandedDocumentId
            )
        }
    }

    fun refreshDocumentsAfterDeletion(deletedIds: Set<String>, deletedCount: Int) {
        _uiState.update { current ->
            val nextDocuments = current.documents.filterNot { it.id in deletedIds }
            current.copy(
                documents = nextDocuments,
                duplicateDocumentIds = duplicateDetector.duplicateDocumentIds(nextDocuments),
                expandedDocumentId = current.expandedDocumentId.takeUnless { it in deletedIds },
                selectedDocumentIds = current.selectedDocumentIds - deletedIds,
                selectionMode = current.selectionMode && (current.selectedDocumentIds - deletedIds).isNotEmpty()
            )
        }
        onMessage(if (deletedCount == 1) "Deleted 1 document." else "Deleted $deletedCount documents.")
    }

    fun applyBackupImport(reload: BackupImportReload) {
        _uiState.update {
            it.copy(
                documents = reload.documents,
                folders = reload.folders,
                duplicateDocumentIds = reload.duplicateDocumentIds,
                expandedDocumentId = null
            )
        }
    }

    fun exportPathsForSelectedDocuments(
        exportPathFor: (ScanDocument) -> String?,
        exportMimeTypeFor: (ScanDocument) -> String
    ): List<Pair<String, String>> {
        val selectedIds = _uiState.value.selectedDocumentIds
        return _uiState.value.documents
            .filter { it.id in selectedIds }
            .mapNotNull { document -> exportPathFor(document)?.let { it to exportMimeTypeFor(document) } }
    }
}
