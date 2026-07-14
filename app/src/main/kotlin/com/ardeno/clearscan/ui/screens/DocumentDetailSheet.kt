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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
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
import com.ardeno.clearscan.ui.components.displayLabel
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
    onShareImages: () -> Unit = {},
    onExportText: () -> Unit = {},
    onExportDocx: () -> Unit = {},
    onPrint: () -> Unit = {},
    onUploadToSelfHost: () -> Unit = {},
    onRedactIdFields: () -> Unit = {},
    onDelete: () -> Unit,
    onRestore: () -> Unit = {},
    onRetryOcr: () -> Unit,
    onOcrLanguageChange: (OcrLanguage) -> Unit = {},
    onRename: (String) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onUpdateTags: (List<String>) -> Unit = {},
    onMoveToFolder: (String?) -> Unit = {},
    onMessage: (String) -> Unit = {},
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
    var showRenameDialog by remember { mutableStateOf(false) }
    var tagsInput by remember(document.id, document.tags) {
        mutableStateOf(document.tags.joinToString(", "))
    }
    val clipboardManager = LocalClipboardManager.current
    val ocrCopiedMessage = stringResource(R.string.msg_ocr_text_copied)
    val performHaptic = rememberClearScanHaptics()

    if (showRenameDialog) {
        RenameDocumentDialog(
            initialTitle = document.title,
            onDismiss = { showRenameDialog = false },
            onRename = { newTitle ->
                performHaptic(ClearScanHaptic.Confirm)
                showRenameDialog = false
                onRename(newTitle)
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    stringResource(
                        if (document.isDeleted) {
                            R.string.document_delete_permanently_title
                        } else {
                            R.string.document_delete_title
                        }
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (document.isDeleted) {
                            R.string.document_delete_permanently_message
                        } else {
                            R.string.document_delete_message
                        },
                        document.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Reject)
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(
                        stringResource(
                            if (document.isDeleted) {
                                R.string.action_delete_permanently
                            } else {
                                R.string.action_delete
                            }
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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
                        contentDescription = stringResource(R.string.library_close_detail)
                    )
                }
            }
            DocumentThumbnailHero(document = document)

            Column(verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.xs)
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.defaultMinSize(
                            minWidth = ClearScanSpacing.minTouchTarget,
                            minHeight = ClearScanSpacing.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.a11y_rename_document)
                        )
                    }
                }
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
                            text = stringResource(R.string.document_possible_duplicate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            GroupedSection(title = stringResource(R.string.document_organize), horizontalPadding = 0.dp) {
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
                        label = {
                            Text(
                                if (document.isFavorite) {
                                    stringResource(R.string.document_favorited)
                                } else {
                                    stringResource(R.string.document_add_favorites)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (document.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (document.isFavorite) {
                                    stringResource(R.string.document_remove_favorites)
                                } else {
                                    stringResource(R.string.document_add_favorites)
                                }
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
                        label = { Text(stringResource(R.string.document_tags_label)) },
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
                        Text(stringResource(R.string.document_save_tags))
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
                if (document.isDeleted) {
                    Button(
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onRestore()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = stringResource(R.string.action_restore))
                        Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.action_restore))
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
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete_permanently))
                        Text(
                            modifier = Modifier.padding(start = ClearScanSpacing.sm),
                            text = stringResource(R.string.action_delete_permanently)
                        )
                    }
                } else {
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
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.a11y_share_document))
                        Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.action_share))
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
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.a11y_delete_document))
                        Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.action_delete))
                    }
                }
            }

            FilledTonalButton(
                onClick = {
                    performHaptic(ClearScanHaptic.Confirm)
                    onShareImages()
                },
                enabled = document.pageImagePaths.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Outlined.Image, contentDescription = stringResource(R.string.a11y_share_images))
                Text(
                    modifier = Modifier.padding(start = ClearScanSpacing.sm),
                    text = stringResource(R.string.document_share_images)
                )
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
                    Icon(Icons.Outlined.TextSnippet, contentDescription = stringResource(R.string.a11y_export_ocr_text))
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.document_export_text))
                }
                FilledTonalButton(
                    onClick = {
                        performHaptic(ClearScanHaptic.Confirm)
                        onExportDocx()
                    },
                    enabled = document.ocrText.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = stringResource(R.string.a11y_export_docx))
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.document_export_docx))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
            ) {
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
                    Icon(Icons.Outlined.Print, contentDescription = stringResource(R.string.a11y_print_document))
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.document_print))
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
                    Icon(Icons.Outlined.CloudUpload, contentDescription = stringResource(R.string.a11y_upload_self_host))
                    Text(
                        modifier = Modifier.padding(start = ClearScanSpacing.sm),
                        text = if (isSelfHostUploading) {
                            stringResource(R.string.document_uploading)
                        } else {
                            stringResource(R.string.document_upload_self_host)
                        }
                    )
                }
            }

            if (document.scanMode == ScanMode.IdCard || document.tags.contains("id-card")) {
                GroupedSection(title = stringResource(R.string.document_id_privacy), horizontalPadding = 0.dp) {
                    Column(
                        modifier = Modifier.padding(ClearScanSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.document_id_privacy_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (idRedactionSuggestion != null) {
                            Text(
                                text = stringResource(
                                    R.string.document_suggested_redaction,
                                    idRedactionSuggestion.labels.joinToString()
                                ),
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
                                Text(stringResource(R.string.document_redact_sensitive))
                            }
                        } else if (document.ocrStatus == OcrStatus.Ready) {
                            Text(
                                text = stringResource(R.string.document_id_ocr_no_flag),
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
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.document_retry_ocr))
                    Text(modifier = Modifier.padding(start = ClearScanSpacing.sm), text = stringResource(R.string.document_retry_ocr))
                }
            }

            document.receiptFields?.takeIf { it.hasAnyField }?.let { receipt ->
                GroupedSection(title = stringResource(R.string.document_receipt_details), horizontalPadding = 0.dp) {
                    Column(
                        modifier = Modifier.padding(ClearScanSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.xs)
                    ) {
                        receipt.merchant?.let { merchant ->
                            Text(
                                text = stringResource(R.string.document_receipt_merchant, merchant),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        receipt.date?.let { date ->
                            Text(
                                text = stringResource(R.string.document_receipt_date, date),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        receipt.amount?.let { amount ->
                            Text(
                                text = stringResource(R.string.document_receipt_amount, amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = stringResource(R.string.document_receipt_extracted_local),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            GroupedSection(title = stringResource(R.string.document_recognized_text), horizontalPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(ClearScanSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
                ) {
                    OcrLanguagePicker(
                        selectedLanguage = document.ocrLanguage,
                        onLanguageSelected = onOcrLanguageChange,
                        titleRes = R.string.document_ocr_language
                    )
                    Text(
                        text = document.ocrText.ifBlank {
                            stringResource(R.string.document_ocr_placeholder)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (document.ocrText.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                performHaptic(ClearScanHaptic.Confirm)
                                clipboardManager.setText(AnnotatedString(document.ocrText))
                                onMessage(ocrCopiedMessage)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.a11y_copy_ocr_text)
                            )
                            Text(
                                modifier = Modifier.padding(start = ClearScanSpacing.sm),
                                text = stringResource(R.string.document_copy_ocr)
                            )
                        }
                    }
                }
            }

            GroupedSection(title = stringResource(R.string.document_pdf_tools), horizontalPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(ClearScanSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        PdfToolButton(modifier = Modifier.weight(1f), label = stringResource(R.string.document_reorder), onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onReorderPages()
                        })
                        PdfToolButton(modifier = Modifier.weight(1f), label = stringResource(R.string.document_delete_pages), onClick = {
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
                                label = { Text(quality.displayLabel()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.document_compress_pdf),
                        onClick = {
                            performHaptic(ClearScanHaptic.Confirm)
                            onCompress()
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
                    ) {
                        PdfToolButton(modifier = Modifier.weight(1f), label = stringResource(R.string.document_split), onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onSplit()
                        })
                        PdfToolButton(modifier = Modifier.weight(1f), label = stringResource(R.string.document_rotate), onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onRotate()
                        })
                        PdfToolButton(modifier = Modifier.weight(1f), label = stringResource(R.string.document_redact), onClick = {
                            performHaptic(ClearScanHaptic.Selection)
                            onRedact()
                        })
                    }

                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.document_annotate_pages),
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
                        label = { Text(stringResource(R.string.document_signature_text)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.document_add_signature),
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
                        label = { Text(stringResource(R.string.document_pdf_password)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    PdfToolButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.document_password_protect),
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

@Composable
private fun RenameDocumentDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var titleInput by remember(initialTitle) { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.document_rename_title)) },
        text = {
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                singleLine = true,
                label = { Text(stringResource(R.string.document_title_label)) },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(titleInput) },
                enabled = titleInput.isNotBlank()
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPicker(
    folders: List<DocumentFolder>,
    selectedFolderId: String?,
    onMoveToFolder: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val noFolderLabel = stringResource(R.string.document_no_folder)
    val selectedFolderName = folders.find { it.id == selectedFolderId }?.name ?: noFolderLabel

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
            label = { Text(stringResource(R.string.document_folder)) },
            leadingIcon = {
                Icon(Icons.Outlined.Folder, contentDescription = stringResource(R.string.document_folder))
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
                text = { Text(noFolderLabel) },
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

@Composable
private fun ScanDocument.exportLabel(): String = stringResource(
    when {
        searchablePdfReady -> R.string.document_export_searchable_pdf
        pdfPath != null -> R.string.document_export_original_pdf
        pageImagePaths.isNotEmpty() -> R.string.document_export_image
        else -> R.string.document_export_none
    }
)
