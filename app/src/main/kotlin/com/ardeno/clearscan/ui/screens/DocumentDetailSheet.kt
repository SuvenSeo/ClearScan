package com.ardeno.clearscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.ocr.IdRedactionSuggestion
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.PdfCompressQuality
import com.ardeno.clearscan.ui.components.ClearScanHaptic
import com.ardeno.clearscan.ui.components.DocumentThumbnailHero
import com.ardeno.clearscan.ui.components.GroupedSection
import com.ardeno.clearscan.ui.components.OcrLanguagePicker
import com.ardeno.clearscan.ui.components.OcrStatusChip
import com.ardeno.clearscan.ui.components.TagChipRow
import com.ardeno.clearscan.ui.components.rememberClearScanHaptics
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import com.ardeno.clearscan.ui.theme.SheetShape
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy · HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailSheet(
    document: ScanDocument,
    folders: List<DocumentFolder> = emptyList(),
    isDuplicate: Boolean = false,
    signatureText: String,
    pdfPassword: String,
    compressQuality: PdfCompressQuality,
    selfHostEnabled: Boolean = false,
    isSelfHostUploading: Boolean = false,
    idRedactionSuggestion: IdRedactionSuggestion? = null,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExportText: () -> Unit = {},
    onPrint: () -> Unit = {},
    onUploadToSelfHost: () -> Unit = {},
    onRedactIdFields: () -> Unit = {},
    onDelete: () -> Unit,
    onRetryOcr: () -> Unit,
    onOcrLanguageChange: (OcrLanguage) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onUpdateTags: (List<String>) -> Unit = {},
    onMoveToFolder: (String?) -> Unit = {},
    onSignatureTextChange: (String) -> Unit,
    onPdfPasswordChange: (String) -> Unit,
    onCompressQualityChange: (PdfCompressQuality) -> Unit,
    onSplit: () -> Unit,
    onRotate: () -> Unit,
    onSign: () -> Unit,
    onRedact: () -> Unit,
    onAnnotate: () -> Unit,
    onReorderPages: () -> Unit,
    onDeletePages: () -> Unit,
    onCompress: () -> Unit,
    onPasswordProtect: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagsInput by remember(document.id, document.tags) {
        mutableStateOf(document.tags.joinToString(", "))
    }
    val performHaptic = rememberClearScanHaptics()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete document?") },
            text = { Text("\"${document.title}\" will be permanently removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Reject)
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = SheetShape,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = ClearScanSpacing.sm, bottom = ClearScanSpacing.xs)
                        .width(36.dp)
                        .height(5.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ClearScanSpacing.xl)
                .padding(bottom = ClearScanSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
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
            DocumentThumbnailHero(document = document)

            Column(verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.xs)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detailDateFormatter.format(document.createdAt.atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)) {
                    OcrStatusChip(status = document.ocrStatus)
                    Text(
                        text = document.exportLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (isDuplicate) {
                        Text(
                            text = "Possible duplicate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            GroupedSection(title = "Organize", horizontalPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(ClearScanSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
                ) {
                    FilterChip(
                        selected = document.isFavorite,
                        onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onToggleFavorite()
                        },
                        label = { Text(if (document.isFavorite) "Favorited" else "Add to favorites") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (document.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (document.isFavorite) "Remove from favorites" else "Add to favorites"
                            )
                        }
                    )

                    FolderPicker(
                        folders = folders,
                        selectedFolderId = document.folderId,
                        onMoveToFolder = onMoveToFolder
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        label = { Text("Tags (comma separated)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    FilledTonalButton(
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            val tags = tagsInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            onUpdateTags(tags)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Save tags")
                    }
                    if (document.tags.isNotEmpty()) {
                        TagChipRow(tags = document.tags)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
            ) {
                Button(
                    onClick = {
                        performHaptic(ClearScanHaptic.Confirm)
                        onShare()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share document")
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = "Share")
                }
                FilledTonalButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete document")
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = "Delete")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
            ) {
                FilledTonalButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Confirm)
                        onExportText()
                    },
                    enabled = document.ocrText.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.TextSnippet, contentDescription = "Export OCR text")
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = "Export text")
                }
                FilledTonalButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Confirm)
                        onPrint()
                    },
                    enabled = document.pdfPath != null || document.searchablePdfPath != null,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = "Print document")
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = "Print")
                }
            }

            if (selfHostEnabled) {
                FilledTonalButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Confirm)
                        onUploadToSelfHost()
                    },
                    enabled = !isSelfHostUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = "Upload to self-host")
                    Text(
                        modifier = Modifier.padding(start = ClearScanSpacing.sm),
                        text = if (isSelfHostUploading) "Uploading…" else "Upload to self-host"
                    )
                }
            }

            if (document.scanMode == ScanMode.IdCard || document.tags.contains("id-card")) {
                GroupedSection(title = "ID privacy", horizontalPadding = 0.dp) {
                    Column(
                        modifier = Modifier.padding(ClearScanSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        Text(
                            text = "No watermark on exports. Review sensitive fields before sharing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (idRedactionSuggestion != null) {
                            Text(
                                text = "Suggested redaction: ${idRedactionSuggestion.labels.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            FilledTonalButton(
                                onClick = {
                                    performHaptic(ClearScanHaptic.Confirm)
                                    onRedactIdFields()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Redact sensitive fields")
                            }
                        } else if (document.ocrStatus == OcrStatus.Ready) {
                            Text(
                                text = "OCR did not flag obvious ID numbers. You can still use header redaction below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (document.ocrStatus == OcrStatus.Failed) {
                FilledTonalButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Selection)
                        onRetryOcr()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Retry OCR")
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = "Retry OCR")
                }
            }

            document.receiptFields?.takeIf { it.hasAnyField }?.let { receipt ->
                GroupedSection(title = "Receipt details", horizontalPadding = 0.dp) {
                    Column(
                        modifier = Modifier.padding(ClearScanSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.xs)
                    ) {
                        receipt.merchant?.let { merchant ->
                            Text(
                                text = "Merchant: $merchant",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        receipt.date?.let { date ->
                            Text(
                                text = "Date: $date",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        receipt.amount?.let { amount ->
                            Text(
                                text = "Amount: $amount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Extracted locally from OCR text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            GroupedSection(title = "Recognized text", horizontalPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(ClearScanSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
                ) {
                    OcrLanguagePicker(
                        selectedLanguage = document.ocrLanguage,
                        onLanguageSelected = onOcrLanguageChange,
                        title = "OCR language for this document"
                    )
                    Text(
                        text = document.ocrText.ifBlank {
                            "OCR text will appear here after the local recognizer finishes processing this document."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            GroupedSection(title = "PDF tools", horizontalPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(ClearScanSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        PdfToolButton(modifier = Modifier.weight(1f), label = "Reorder", onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onReorderPages()
                        })
                        PdfToolButton(modifier = Modifier.weight(1f), label = "Delete pages", onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onDeletePages()
                        })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        PdfCompressQuality.entries.forEach { quality ->
                            FilterChip(
                                selected = compressQuality == quality,
                                onClick = {
                                    performHaptic(ClearScanHaptic.Selection)
                                    onCompressQualityChange(quality)
                                },
                                label = { Text(quality.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Compress PDF",
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onCompress()
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        PdfToolButton(modifier = Modifier.weight(1f), label = "Split", onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onSplit()
                        })
                        PdfToolButton(modifier = Modifier.weight(1f), label = "Rotate", onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onRotate()
                        })
                        PdfToolButton(modifier = Modifier.weight(1f), label = "Redact", onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onRedact()
                        })
                    }

                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Annotate pages",
                        onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onAnnotate()
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = signatureText,
                        onValueChange = onSignatureTextChange,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        label = { Text("Signature text") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Add signature",
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onSign()
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = pdfPassword,
                        onValueChange = onPdfPasswordChange,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("PDF password") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Password protect",
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onPasswordProtect()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPicker(
    folders: List<DocumentFolder>,
    selectedFolderId: String?,
    onMoveToFolder: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFolderName = folders.find { it.id == selectedFolderId }?.name ?: "No folder"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = selectedFolderName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Folder") },
            leadingIcon = {
                Icon(Icons.Outlined.Folder, contentDescription = "Folder")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("No folder") },
                onClick = {
                    onMoveToFolder(null)
                    expanded = false
                }
            )
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name) },
                    onClick = {
                        onMoveToFolder(folder.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PdfToolButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        modifier = modifier.defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = ClearScanSpacing.md, vertical = 10.dp)
    ) {
        Text(text = label)
    }
}

private fun ScanDocument.exportLabel(): String = when {
    searchablePdfReady -> "Searchable PDF ready"
    pdfPath != null -> "Original PDF ready"
    pageImagePaths.isNotEmpty() -> "Image export ready"
    else -> "No export available"
}
