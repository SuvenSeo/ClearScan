package com.ardeno.clearscan.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ardeno.clearscan.model.NormalizedPoint
import com.ardeno.clearscan.model.NormalizedRect
import com.ardeno.clearscan.model.PageAnnotation
import com.ardeno.clearscan.model.ScanDocument
import java.io.File
import kotlin.math.min

enum class AnnotatorTool {
    Signature,
    Highlight,
    Redact,
    Note
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentAnnotatorScreen(
    document: ScanDocument,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onApply: (Map<Int, List<PageAnnotation>>) -> Unit
) {
    val pagePaths = remember(document.id) {
        document.pageImagePaths.filter { File(it).exists() }
    }
    if (pagePaths.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cannot annotate") },
            text = { Text("This document has no page images to annotate.") },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { pagePaths.size })
    var selectedTool by remember { mutableStateOf(AnnotatorTool.Signature) }
    val annotationsByPage = remember(document.id) {
        mutableStateMapOf<Int, MutableList<PageAnnotation>>()
    }
    var pendingNoteAnchor by remember { mutableStateOf<NormalizedPoint?>(null) }
    var noteDraft by remember { mutableStateOf("") }

    fun pageAnnotations(pageIndex: Int): MutableList<PageAnnotation> =
        annotationsByPage.getOrPut(pageIndex) { mutableStateListOf() }

    fun undoLast(pageIndex: Int) {
        val pageItems = annotationsByPage[pageIndex]
        if (!pageItems.isNullOrEmpty()) {
            pageItems.removeAt(pageItems.lastIndex)
        }
    }

    val totalAnnotations = annotationsByPage.values.sumOf { it.size }

    if (pendingNoteAnchor != null) {
        AlertDialog(
            onDismissRequest = {
                pendingNoteAnchor = null
                noteDraft = ""
            },
            title = { Text("Add note") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Note text") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val anchor = pendingNoteAnchor ?: return@TextButton
                        if (noteDraft.isNotBlank()) {
                            pageAnnotations(pagerState.currentPage).add(
                                PageAnnotation.StickyNote(anchor = anchor, text = noteDraft.trim())
                            )
                        }
                        pendingNoteAnchor = null
                        noteDraft = ""
                    },
                    enabled = noteDraft.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingNoteAnchor = null
                        noteDraft = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Annotate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Page ${pagerState.currentPage + 1} of ${pagePaths.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, enabled = !isApplying) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { undoLast(pagerState.currentPage) },
                        enabled = !isApplying && pageAnnotations(pagerState.currentPage).isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Undo, contentDescription = "Undo")
                    }
                    Button(
                        onClick = { onApply(annotationsByPage.mapValues { it.value.toList() }) },
                        enabled = !isApplying && totalAnnotations > 0,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text("Apply")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnnotatorToolButton(
                    label = "Sign",
                    icon = Icons.Outlined.Draw,
                    selected = selectedTool == AnnotatorTool.Signature,
                    onClick = { selectedTool = AnnotatorTool.Signature }
                )
                AnnotatorToolButton(
                    label = "Highlight",
                    icon = Icons.Outlined.Highlight,
                    selected = selectedTool == AnnotatorTool.Highlight,
                    onClick = { selectedTool = AnnotatorTool.Highlight }
                )
                AnnotatorToolButton(
                    label = "Redact",
                    icon = Icons.Outlined.Block,
                    selected = selectedTool == AnnotatorTool.Redact,
                    onClick = { selectedTool = AnnotatorTool.Redact }
                )
                AnnotatorToolButton(
                    label = "Note",
                    icon = Icons.Outlined.StickyNote2,
                    selected = selectedTool == AnnotatorTool.Note,
                    onClick = { selectedTool = AnnotatorTool.Note }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1,
            userScrollEnabled = !isApplying
        ) { pageIndex ->
            AnnotatedPageCanvas(
                pagePath = pagePaths[pageIndex],
                annotations = pageAnnotations(pageIndex),
                selectedTool = selectedTool,
                enabled = !isApplying,
                onAddAnnotation = { annotation ->
                    pageAnnotations(pageIndex).add(annotation)
                },
                onRequestNote = { anchor ->
                    pendingNoteAnchor = anchor
                    noteDraft = ""
                }
            )
        }
    }
}

@Composable
private fun AnnotatorToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.background(containerColor, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = contentColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
private fun AnnotatedPageCanvas(
    pagePath: String,
    annotations: List<PageAnnotation>,
    selectedTool: AnnotatorTool,
    enabled: Boolean,
    onAddAnnotation: (PageAnnotation) -> Unit,
    onRequestNote: (NormalizedPoint) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageSize = remember(pagePath) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(pagePath, options)
        Size(options.outWidth.toFloat(), options.outHeight.toFloat())
    }
    var activeStroke by remember { mutableStateOf<List<NormalizedPoint>>(emptyList()) }
    var activeRect by remember { mutableStateOf<NormalizedRect?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val fitRect = remember(maxWidth, maxHeight, imageSize, density) {
            with(density) {
                fitImageRect(
                    containerWidth = maxWidth.toPx(),
                    containerHeight = maxHeight.toPx(),
                    imageWidth = imageSize.width,
                    imageHeight = imageSize.height
                )
            }
        }

        Box(
            modifier = Modifier
                .size(
                    width = with(density) { fitRect.width.toDp() },
                    height = with(density) { fitRect.height.toDp() }
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(pagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Page",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTool, enabled, fitRect.width, fitRect.height) {
                        if (!enabled) return@pointerInput

                        when (selectedTool) {
                            AnnotatorTool.Signature -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        toNormalizedPoint(offset, fitRect.width, fitRect.height)?.let { point ->
                                            activeStroke = listOf(point)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        toNormalizedPoint(change.position, fitRect.width, fitRect.height)?.let { point ->
                                            activeStroke = activeStroke + point
                                        }
                                    },
                                    onDragEnd = {
                                        if (activeStroke.size >= 2) {
                                            onAddAnnotation(
                                                PageAnnotation.FreehandSignature(points = activeStroke)
                                            )
                                        }
                                        activeStroke = emptyList()
                                    },
                                    onDragCancel = { activeStroke = emptyList() }
                                )
                            }
                            AnnotatorTool.Highlight, AnnotatorTool.Redact -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        toNormalizedPoint(offset, fitRect.width, fitRect.height)?.let { point ->
                                            activeRect = NormalizedRect(point.x, point.y, point.x, point.y)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val start = activeRect ?: return@detectDragGestures
                                        toNormalizedPoint(change.position, fitRect.width, fitRect.height)?.let { point ->
                                            activeRect = start.copy(right = point.x, bottom = point.y)
                                        }
                                    },
                                    onDragEnd = {
                                        val rect = activeRect?.normalized()?.takeIf { it.isLargeEnough() }
                                        if (rect != null) {
                                            val annotation = when (selectedTool) {
                                                AnnotatorTool.Highlight -> PageAnnotation.Highlight(rect)
                                                AnnotatorTool.Redact -> PageAnnotation.Redaction(rect)
                                                else -> return@detectDragGestures
                                            }
                                            onAddAnnotation(annotation)
                                        }
                                        activeRect = null
                                    },
                                    onDragCancel = { activeRect = null }
                                )
                            }
                            AnnotatorTool.Note -> {
                                detectTapGestures { offset ->
                                    toNormalizedPoint(offset, fitRect.width, fitRect.height)?.let(onRequestNote)
                                }
                            }
                        }
                    }
            ) {
                annotations.forEach { annotation ->
                    when (annotation) {
                        is PageAnnotation.FreehandSignature -> {
                            if (annotation.points.size < 2) return@forEach
                            val path = Path().apply {
                                val first = annotation.points.first()
                                moveTo(first.x * size.width, first.y * size.height)
                                annotation.points.drop(1).forEach { point ->
                                    lineTo(point.x * size.width, point.y * size.height)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF203048),
                                style = Stroke(
                                    width = min(size.width, size.height) * annotation.strokeWidthRatio,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                        is PageAnnotation.Highlight -> {
                            val rect = annotation.rect.normalized()
                            drawRect(
                                color = Color(0x60FFEB3B),
                                topLeft = Offset(rect.left * size.width, rect.top * size.height),
                                size = Size(
                                    (rect.right - rect.left) * size.width,
                                    (rect.bottom - rect.top) * size.height
                                )
                            )
                        }
                        is PageAnnotation.Redaction -> {
                            val rect = annotation.rect.normalized()
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(rect.left * size.width, rect.top * size.height),
                                size = Size(
                                    (rect.right - rect.left) * size.width,
                                    (rect.bottom - rect.top) * size.height
                                )
                            )
                        }
                        is PageAnnotation.StickyNote -> {
                            val shortEdge = min(size.width, size.height)
                            val noteWidth = shortEdge * 0.34f
                            val noteHeight = shortEdge * 0.16f
                            val left = (annotation.anchor.x * size.width - noteWidth / 2f).coerceIn(0f, size.width - noteWidth)
                            val top = (annotation.anchor.y * size.height - noteHeight / 2f).coerceIn(0f, size.height - noteHeight)
                            drawRoundRect(
                                color = Color(0xFFFFF4A3),
                                topLeft = Offset(left, top),
                                size = Size(noteWidth, noteHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(shortEdge * 0.02f)
                            )
                        }
                    }
                }

                if (activeStroke.size >= 2) {
                    val path = Path().apply {
                        val first = activeStroke.first()
                        moveTo(first.x * size.width, first.y * size.height)
                        activeStroke.drop(1).forEach { point ->
                            lineTo(point.x * size.width, point.y * size.height)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF203048),
                        style = Stroke(
                            width = min(size.width, size.height) * 0.004f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }

                activeRect?.let { rect ->
                    val normalized = rect.normalized()
                    val color = when (selectedTool) {
                        AnnotatorTool.Redact -> Color.Black.copy(alpha = 0.75f)
                        else -> Color(0x80FFEB3B)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(normalized.left * size.width, normalized.top * size.height),
                        size = Size(
                            (normalized.right - normalized.left) * size.width,
                            (normalized.bottom - normalized.top) * size.height
                        )
                    )
                }
            }
        }
    }
}

private fun fitImageRect(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): Rect {
    if (imageWidth <= 0f || imageHeight <= 0f) {
        return Rect(0f, 0f, containerWidth, containerHeight)
    }
    val scale = min(containerWidth / imageWidth, containerHeight / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    val left = (containerWidth - width) / 2f
    val top = (containerHeight - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun toNormalizedPoint(
    offset: Offset,
    width: Float,
    height: Float
): NormalizedPoint? {
    if (width <= 0f || height <= 0f) return null
    if (offset.x < 0f || offset.y < 0f || offset.x > width || offset.y > height) return null
    return NormalizedPoint(
        x = (offset.x / width).coerceIn(0f, 1f),
        y = (offset.y / height).coerceIn(0f, 1f)
    )
}
