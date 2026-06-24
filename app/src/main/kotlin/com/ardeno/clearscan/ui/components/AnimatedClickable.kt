package com.ardeno.clearscan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import com.ardeno.clearscan.ui.theme.ClearScanMotion

fun Modifier.animatedClickable(
    enabled: Boolean = true,
    haptic: ClearScanHaptic? = ClearScanHaptic.LightTap,
    scaleDown: Float = ClearScanMotion.pressScale,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = ClearScanMotion.springSnappy,
        label = "pressScale"
    )
    val performHaptic = rememberClearScanHaptics()

    this
        .scale(scale)
        .clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic?.let(performHaptic)
                onClick()
            }
        )
}
