package com.ardeno.clearscan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.R
import com.ardeno.clearscan.ui.theme.ClearScanMotion

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = selectedCount > 0

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = ClearScanMotion.springSnappyOffset,
            initialOffsetY = { it }
        ),
        exit = slideOutVertically(
            animationSpec = ClearScanMotion.springStiffOffset,
            targetOffsetY = { it }
        )
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.selection_exit)
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.library_selected_count,
                            selectedCount,
                            selectedCount
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onSelectAll,
                        enabled = selectedCount > 0
                    ) {
                        Text(stringResource(R.string.filter_all))
                    }
                    FilledTonalButton(
                        onClick = onMerge,
                        enabled = selectedCount >= 2
                    ) {
                        Icon(
                            Icons.Outlined.MergeType,
                            contentDescription = stringResource(R.string.a11y_merge_selected)
                        )
                    }
                    FilledTonalButton(
                        onClick = onExport,
                        enabled = selectedCount > 0
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.a11y_share_selected)
                        )
                    }
                    FilledTonalButton(
                        onClick = onDelete,
                        enabled = selectedCount > 0
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.a11y_delete_selected)
                        )
                    }
                }
            }
        }
    }
}
