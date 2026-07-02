package com.ardeno.clearscan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val iconScale = remember { Animatable(0.2f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconRotation = remember { Animatable(-12f) }

    val ringAlpha = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            launch {
                iconAlpha.animateTo(1f, animationSpec = tween(300))
            }
            launch {
                iconScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                iconRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                delay(200)
                ringAlpha.animateTo(
                    targetValue = 0.6f,
                    animationSpec = tween(600)
                )
            }
        }
    }

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
            Box(
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = iconScale.value,
                            scaleY = iconScale.value,
                            alpha = iconAlpha.value,
                            rotationZ = iconRotation.value
                        ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.35f)
                        .alpha(ringAlpha.value * 0.2f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
