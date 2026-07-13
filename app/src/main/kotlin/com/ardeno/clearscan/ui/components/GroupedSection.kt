package com.ardeno.clearscan.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ui.theme.ClearScanElevation
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import com.ardeno.clearscan.ui.theme.GroupedSectionShape

@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    horizontalPadding: Dp = ClearScanSpacing.groupedInset,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val sectionElevation = if (isDark) ClearScanElevation.none else ClearScanElevation.card

    Column(modifier = modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = horizontalPadding + ClearScanSpacing.lg,
                    end = horizontalPadding,
                    bottom = ClearScanSpacing.sm
                )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = GroupedSectionShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = sectionElevation
        ) {
            Column(content = content)
        }

        footer?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = horizontalPadding + ClearScanSpacing.lg,
                    end = horizontalPadding,
                    top = ClearScanSpacing.sm
                )
            )
        }
    }
}

@Composable
fun GroupedRowDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 88.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}
