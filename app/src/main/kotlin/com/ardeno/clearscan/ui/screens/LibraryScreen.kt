package com.ardeno.clearscan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.LibraryViewMode
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.components.ClearScanHaptic
import com.ardeno.clearscan.ui.components.DocumentGridItem
import com.ardeno.clearscan.ui.components.EmptyState
import com.ardeno.clearscan.ui.components.FolderFilterRow
import com.ardeno.clearscan.ui.components.GroupedSection
import com.ardeno.clearscan.ui.components.IosSearchField
import com.ardeno.clearscan.ui.components.LibraryGridSkeleton
import com.ardeno.clearscan.ui.components.LibraryListSkeleton
import com.ardeno.clearscan.ui.components.SelectionActionBar
import com.ardeno.clearscan.ui.components.SwipeableDocumentRow
import com.ardeno.clearscan.ui.components.rememberClearScanHaptics
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<ScanDocument>,
    folders: List<DocumentFolder>,
    selectedFolderId: String?,
    showFavoritesOnly: Boolean,
    selectionMode: Boolean,
    selectedDocumentIds: Set<String>,
    duplicateDocumentIds: Set<String>,
    query: String,
    viewMode: LibraryViewMode,
    isWorking: Boolean,
    showEmptySearch: Boolean = false,
    showEmptyFolderFilter: Boolean = false,
    expandedDocumentId: String? = null,
    expandedDocument: ScanDocument? = null,
    onCloseDetailPane: () -> Unit = {},
    onQueryChange: (String) -> Unit,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onSelectAllDocuments: () -> Unit,
    onSelectFolder: (String?) -> Unit,
    onSelectFavorites: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleDocumentSelection: (String) -> Unit,
    onSelectAllVisible: () -> Unit,
    onMergeSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onScanClick: () -> Unit,
    onIdScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onDocumentClick: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val performHaptic = rememberClearScanHaptics()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(true) }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag("library_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectionMode) "Select documents" else "Library",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                selectionMode -> "${selectedDocumentIds.size} selected"
                                documents.isEmpty() -> "No documents yet"
                                else -> "${documents.size} document${if (documents.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (!selectionMode && documents.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                performHaptic(ClearScanHaptic.Selection)
                                onEnterSelectionMode()
                            },
                            modifier = Modifier.defaultMinSize(
                                minWidth = ClearScanSpacing.minTouchTarget,
                                minHeight = ClearScanSpacing.minTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = "Select documents"
                            )
                        }
                    }
                    if (!selectionMode) {
                        IconButton(
                            onClick = {
                                performHaptic(ClearScanHaptic.LightTap)
                                showSearchField = !showSearchField
                            },
                            modifier = Modifier.defaultMinSize(
                                minWidth = ClearScanSpacing.minTouchTarget,
                                minHeight = ClearScanSpacing.minTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = if (showSearchField) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                                contentDescription = if (showSearchField) "Hide search" else "Show search"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            performHaptic(ClearScanHaptic.LightTap)
                            onImportClick()
                        },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ClearScanSpacing.minTouchTarget,
                            minHeight = ClearScanSpacing.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = "Import PDF or images"
                        )
                    }
                    IconButton(
                        onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onIdScanClick()
                        },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ClearScanSpacing.minTouchTarget,
                            minHeight = ClearScanSpacing.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = "Scan ID or passport"
                        )
                    }
                    IconButton(
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onScanClick()
                        },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ClearScanSpacing.minTouchTarget,
                            minHeight = ClearScanSpacing.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = "Scan document"
                        )
                    }
                    IconButton(
                        onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            val nextMode = when (viewMode) {
                                LibraryViewMode.List -> LibraryViewMode.Grid
                                LibraryViewMode.Grid -> LibraryViewMode.List
                            }
                            onViewModeChange(nextMode)
                        },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ClearScanSpacing.minTouchTarget,
                            minHeight = ClearScanSpacing.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = when (viewMode) {
                                LibraryViewMode.List -> Icons.Outlined.GridView
                                LibraryViewMode.Grid -> Icons.Outlined.ViewList
                            },
                            contentDescription = when (viewMode) {
                                LibraryViewMode.List -> "Switch to grid view"
                                LibraryViewMode.Grid -> "Switch to list view"
                            }
                        )
                    }
                    AnimatedVisibility(visible = isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = ClearScanSpacing.lg)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (selectionMode) {
                SelectionActionBar(
                    selectedCount = selectedDocumentIds.size,
                    onSelectAll = onSelectAllVisible,
                    onMerge = onMergeSelected,
                    onExport = onExportSelected,
                    onDelete = onDeleteSelected,
                    onClose = onExitSelectionMode
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isWide = maxWidth >= 600.dp
            val showDetailPane = isWide && expandedDocumentId != null && expandedDocument != null

            if (showDetailPane) {
                Row(Modifier.fillMaxSize()) {
                    // Left pane: list content (40%)
                    Column(Modifier.fillMaxHeight().fillMaxWidth(0.4f)) {
                        AnimatedVisibility(visible = showSearchField) {
                            IosSearchField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier.padding(
                                    horizontal = ClearScanSpacing.lg,
                                    vertical = ClearScanSpacing.sm
                                ),
                                placeholder = "Search titles, tags, or OCR text"
                            )
                        }

                        FolderFilterRow(
                            folders = folders,
                            selectedFolderId = selectedFolderId,
                            showFavoritesOnly = showFavoritesOnly,
                            onSelectAll = { onSelectFolder(null) },
                            onSelectFavorites = onSelectFavorites,
                            onSelectFolder = onSelectFolder,
                            onCreateFolder = { showCreateFolderDialog = true }
                        )

                        LibraryContentPane(
                            documents = documents,
                            isWorking = isWorking,
                            showEmptySearch = showEmptySearch,
                            viewMode = viewMode,
                            selectedFolderId = selectedFolderId,
                            showFavoritesOnly = showFavoritesOnly,
                            selectionMode = selectionMode,
                            selectedDocumentIds = selectedDocumentIds,
                            duplicateDocumentIds = duplicateDocumentIds,
                            onDocumentClick = onDocumentClick,
                            onDeleteDocument = onDeleteDocument,
                            onToggleDocumentSelection = onToggleDocumentSelection,
                            onScanClick = {
                                performHaptic(ClearScanHaptic.Confirm)
                                onScanClick()
                            },
                            onImportClick = {
                                performHaptic(ClearScanHaptic.LightTap)
                                onImportClick()
                            },
                            onPerformHaptic = performHaptic,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Divider
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Right pane: document detail (60%)
                    DocumentDetailPane(
                        document = expandedDocument,
                        onClose = onCloseDetailPane,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.6f)
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = showSearchField) {
                        IosSearchField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.padding(
                                horizontal = ClearScanSpacing.lg,
                                vertical = ClearScanSpacing.sm
                            ),
                            placeholder = "Search titles, tags, or OCR text"
                        )
                    }

                    FolderFilterRow(
                        folders = folders,
                        selectedFolderId = selectedFolderId,
                        showFavoritesOnly = showFavoritesOnly,
                        onSelectAll = { onSelectFolder(null) },
                        onSelectFavorites = onSelectFavorites,
                        onSelectFolder = onSelectFolder,
                        onCreateFolder = { showCreateFolderDialog = true }
                    )

                    LibraryContentPane(
                        documents = documents,
                        isWorking = isWorking,
                        showEmptySearch = showEmptySearch,
                        viewMode = viewMode,
                        selectedFolderId = selectedFolderId,
                        showFavoritesOnly = showFavoritesOnly,
                        selectionMode = selectionMode,
                        selectedDocumentIds = selectedDocumentIds,
                        duplicateDocumentIds = duplicateDocumentIds,
                        onDocumentClick = onDocumentClick,
                        onDeleteDocument = onDeleteDocument,
                        onToggleDocumentSelection = onToggleDocumentSelection,
                        onScanClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onScanClick()
                        },
                        onImportClick = {
                            performHaptic(ClearScanHaptic.LightTap)
                            onImportClick()
                        },
                        onPerformHaptic = performHaptic,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private enum class LibraryContentState {
    Loading,
    EmptySearch,
    EmptyLibrary,
    EmptyFolderFilter,
    Grid,
    List
}

@Composable
private fun LibraryContentPane(
    documents: List<ScanDocument>,
    isWorking: Boolean,
    showEmptySearch: Boolean,
    viewMode: LibraryViewMode,
    selectedFolderId: String?,
    showFavoritesOnly: Boolean,
    selectionMode: Boolean,
    selectedDocumentIds: Set<String>,
    duplicateDocumentIds: Set<String>,
    onDocumentClick: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onToggleDocumentSelection: (String) -> Unit,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onPerformHaptic: (ClearScanHaptic) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFolderFilter = selectedFolderId != null || showFavoritesOnly
    val showEmptyFolderFilter = documents.isEmpty() && hasFolderFilter && !showEmptySearch && !isWorking

    AnimatedContent(
        targetState = when {
            isWorking && documents.isEmpty() -> LibraryContentState.Loading
            documents.isEmpty() && showEmptySearch -> LibraryContentState.EmptySearch
            documents.isEmpty() && showEmptyFolderFilter -> LibraryContentState.EmptyFolderFilter
            documents.isEmpty() -> LibraryContentState.EmptyLibrary
            viewMode == LibraryViewMode.Grid -> LibraryContentState.Grid
            else -> LibraryContentState.List
        },
        transitionSpec = {
            fadeIn(ClearScanMotion.fadeMedium) togetherWith fadeOut(ClearScanMotion.fadeFast)
        },
        label = "libraryContent",
        modifier = modifier
    ) { state ->
        when (state) {
            LibraryContentState.Loading -> {
                if (viewMode == LibraryViewMode.Grid) {
                    LibraryGridSkeleton(modifier = Modifier.fillMaxSize())
                } else {
                    LibraryListSkeleton(modifier = Modifier.fillMaxSize())
                }
            }
            LibraryContentState.EmptySearch -> EmptySearchState()
            LibraryContentState.EmptyFolderFilter -> EmptyFolderFilterState()
            LibraryContentState.EmptyLibrary -> {
                EmptyLibraryState(
                    onScanClick = {
                        onPerformHaptic(ClearScanHaptic.Confirm)
                        onScanClick()
                    },
                    onImportClick = {
                        onPerformHaptic(ClearScanHaptic.LightTap)
                        onImportClick()
                    }
                )
            }
            LibraryContentState.Grid -> {
                LibraryGridContent(
                    documents = documents,
                    selectionMode = selectionMode,
                    selectedDocumentIds = selectedDocumentIds,
                    duplicateDocumentIds = duplicateDocumentIds,
                    onDocumentClick = onDocumentClick,
                    onToggleDocumentSelection = onToggleDocumentSelection
                )
            }
            LibraryContentState.List -> {
                LibraryListContent(
                    documents = documents,
                    selectionMode = selectionMode,
                    selectedDocumentIds = selectedDocumentIds,
                    duplicateDocumentIds = duplicateDocumentIds,
                    onDocumentClick = onDocumentClick,
                    onDeleteDocument = onDeleteDocument,
                    onToggleDocumentSelection = onToggleDocumentSelection
                )
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                singleLine = true,
                label = { Text("Folder name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LibraryListContent(
    documents: List<ScanDocument>,
    selectionMode: Boolean,
    selectedDocumentIds: Set<String>,
    duplicateDocumentIds: Set<String>,
    onDocumentClick: (ScanDocument) -> Unit,
    onDeleteDocument: (ScanDocument) -> Unit,
    onToggleDocumentSelection: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GroupedSection {
                documents.forEachIndexed { index, document ->
                    SwipeableDocumentRow(
                        document = document,
                        onClick = { onDocumentClick(document) },
                        onDelete = { onDeleteDocument(document) },
                        showDivider = index < documents.lastIndex,
                        selectionMode = selectionMode,
                        isSelected = document.id in selectedDocumentIds,
                        isDuplicate = document.id in duplicateDocumentIds,
                        onSelectionToggle = { onToggleDocumentSelection(document.id) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(ClearScanSpacing.lg))
        }
    }
}

@Composable
private fun LibraryGridContent(
    documents: List<ScanDocument>,
    selectionMode: Boolean,
    selectedDocumentIds: Set<String>,
    duplicateDocumentIds: Set<String>,
    onDocumentClick: (ScanDocument) -> Unit,
    onToggleDocumentSelection: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = ClearScanSpacing.md,
            vertical = ClearScanSpacing.sm
        ),
        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
    ) {
        items(
            items = documents,
            key = { it.id }
        ) { document ->
            DocumentGridItem(
                document = document,
                onClick = { onDocumentClick(document) },
                selectionMode = selectionMode,
                isSelected = document.id in selectedDocumentIds,
                isDuplicate = document.id in duplicateDocumentIds,
                onSelectionToggle = { onToggleDocumentSelection(document.id) }
            )
        }
    }
}

@Composable
private fun EmptySearchState() {
    EmptyState(
        icon = Icons.Outlined.SearchOff,
        title = "No matching documents",
        message = "Try searching by title, tag, or text recognized from a scan."
    )
}

@Composable
private fun EmptyLibraryState(
    onScanClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ClearScanSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.lg)
    ) {
        EmptyState(
            icon = Icons.Outlined.Description,
            title = "Your document library is empty",
            message = "Scan a receipt, contract, or note. OCR runs locally so you can search text later."
        )
        Button(
            onClick = onScanClick,
            modifier = Modifier.defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Rounded.DocumentScanner,
                contentDescription = "Scan document"
            )
            Text(
                modifier = Modifier.padding(start = ClearScanSpacing.sm),
                text = "Scan your first document"
            )
        }
        FilledTonalButton(
            onClick = onImportClick,
            modifier = Modifier.defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Outlined.FileUpload,
                contentDescription = "Import PDF or images"
            )
            Text(
                modifier = Modifier.padding(start = ClearScanSpacing.sm),
                text = "Import PDF or images"
            )
        }
    }
}

@Composable
private fun EmptyFolderFilterState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ClearScanSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.lg)
    ) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = "No documents in this filter",
            message = "Try selecting a different folder or clearing the filter to see all documents."
        )
    }
}

@Composable
private fun DocumentDetailPane(
    document: ScanDocument,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(ClearScanSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.defaultMinSize(
                    minWidth = ClearScanSpacing.minTouchTarget,
                    minHeight = ClearScanSpacing.minTouchTarget
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close document detail"
                )
            }
        }
    }
}
