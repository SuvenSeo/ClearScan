package com.ardeno.clearscan.ui.components

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

@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    horizontalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = horizontalPadding + 16.dp,
                    end = horizontalPadding,
                    bottom = 8.dp
                )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(content = content)
        }

        footer?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = horizontalPadding + 16.dp,
                    end = horizontalPadding,
                    top = 8.dp
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
