package com.ardeno.clearscan.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.model.ScanDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDocumentRow(
    document: ScanDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isDeleting = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDeleting) MaterialTheme.colorScheme.error else Color.Transparent
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isDeleting) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete ${document.title}",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        content = {
            DocumentCard(
                document = document,
                onClick = onClick,
                showDivider = showDivider,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    )
}
