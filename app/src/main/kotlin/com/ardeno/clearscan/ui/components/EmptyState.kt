package com.ardeno.clearscan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    visible: Boolean = true
) {
    val performHaptic = rememberClearScanHaptics()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(ClearScanMotion.fadeMedium) +
            slideInVertically(
                animationSpec = ClearScanMotion.springGentleOffset,
                initialOffsetY = { it / 4 }
            )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = ClearScanSpacing.xxxl, horizontal = ClearScanSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = ClearScanSpacing.sm)
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(ClearScanSpacing.sm))
                Button(
                    onClick = {
                        performHaptic(ClearScanHaptic.Confirm)
                        onAction()
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
