package com.ardeno.clearscan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDocumentRow(
    document: ScanDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDuplicate: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null
) {
    if (selectionMode) {
        DocumentCard(
            document = document,
            onClick = onClick,
            showDivider = showDivider,
            selectionMode = true,
            isSelected = isSelected,
            isDuplicate = isDuplicate,
            onSelectionToggle = onSelectionToggle,
            modifier = modifier
        )
        return
    }

    val performHaptic = rememberClearScanHaptics()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                performHaptic(ClearScanHaptic.Reject)
                onDelete()
                true
            } else {
                false
            }
        }
    )

    val isDeleting = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val backgroundColor by animateColorAsState(
        targetValue = if (isDeleting) {
            MaterialTheme.colorScheme.error
        } else {
            Color.Transparent
        },
        animationSpec = ClearScanMotion.springStiffColor,
        label = "swipeBackground"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isDeleting) 1f else 0.6f,
        animationSpec = ClearScanMotion.springSnappy,
        label = "deleteIconScale"
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = ClearScanSpacing.xxl),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.a11y_delete_document_title, document.title),
                    tint = if (isDeleting) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .scale(iconScale)
                )
            }
        },
        content = {
            DocumentCard(
                document = document,
                onClick = onClick,
                showDivider = showDivider,
                isDuplicate = isDuplicate,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    )
}
