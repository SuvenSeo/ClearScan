package com.ardeno.clearscan.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Spring and tween specs matching iOS-like motion on Android. */
object ClearScanMotion {

    // ── Spring stiffness presets (from Spring.Stiffness*) ──────────────────
    /** Stiffest — immediate response for interactive elements. */
    const val stiffnessInteractive = Spring.StiffnessHigh
    /** Medium-stiff — standard spring animations. */
    const val stiffnessStandard = Spring.StiffnessMedium
    /** Low — gentle, slow-moving animations. */
    const val stiffnessGentle = Spring.StiffnessLow
    /** Very low — very slow spring motion. */
    const val stiffnessVeryLow = Spring.StiffnessVeryLow

    // ── Spring damping presets (from Spring.DampingRatio*) ─────────────────
    /** No bounce — critically damped. */
    const val dampingNoBounce = Spring.DampingRatioNoBouncy
    /** Slight bounce — medium bouncy. */
    const val dampingSlightBounce = Spring.DampingRatioMediumBouncy
    /** Moderate bounce — low bouncy. */
    const val dampingModerateBounce = Spring.DampingRatioLowBouncy
    /** High bounce — very bouncy. */
    const val dampingHighBounce = Spring.DampingRatioHighBouncy

    // ── Preset spring animation specs ──────────────────────────────────────

    /** Snappy, minimal bounce for buttons and press feedback. */
    val springSnappy = spring<Float>(
        dampingRatio = dampingSlightBounce,
        stiffness = stiffnessStandard
    )

    /** Snappy spring for offset transitions. */
    val springSnappyOffset = spring<IntOffset>(
        dampingRatio = dampingSlightBounce,
        stiffness = stiffnessStandard
    )

    /** Snappy spring for size transitions. */
    val springSnappySize = spring<IntSize>(
        dampingRatio = dampingSlightBounce,
        stiffness = stiffnessStandard
    )

    /** Gentle, soft spring for appearance animations. */
    val springGentle = spring<Float>(
        dampingRatio = dampingModerateBounce,
        stiffness = stiffnessGentle
    )

    /** Gentle spring for offset transitions. */
    val springGentleOffset = spring<IntOffset>(
        dampingRatio = dampingModerateBounce,
        stiffness = stiffnessGentle
    )

    /** Gentle spring for size transitions. */
    val springGentleSize = spring<IntSize>(
        dampingRatio = dampingModerateBounce,
        stiffness = stiffnessGentle
    )

    /** Gentle spring for Dp properties. */
    val springGentleDp = spring<Dp>(
        dampingRatio = dampingModerateBounce,
        stiffness = stiffnessGentle
    )

    /** Snappy spring for Dp properties. */
    val springSnappyDp = spring<Dp>(
        dampingRatio = dampingSlightBounce,
        stiffness = stiffnessStandard
    )

    /** Stiff, no-bounce for immediate visual feedback (dragging, resizing). */
    val springStiff = spring<Float>(
        dampingRatio = dampingNoBounce,
        stiffness = stiffnessStandard
    )

    /** Stiff, no-bounce spring for offset transitions. */
    val springStiffOffset = spring<IntOffset>(
        dampingRatio = dampingNoBounce,
        stiffness = stiffnessStandard
    )

    /** Stiff, no-bounce spring for color transitions. */
    val springStiffColor = spring<Color>(
        dampingRatio = dampingNoBounce,
        stiffness = stiffnessStandard
    )

    // ── Additional composed presets ─────────────────────────────────────────

    /** Bouncy spring for playful entrance animations. */
    val springBouncy = spring<Float>(
        dampingRatio = dampingHighBounce,
        stiffness = stiffnessStandard
    )

    /** Interactive spring for drag gestures and follow-through. */
    val springInteractive = spring<Float>(
        dampingRatio = dampingNoBounce,
        stiffness = stiffnessInteractive
    )

    /** Quick moderate-bounce spring for list items. */
    val springListItem = spring<Float>(
        dampingRatio = dampingSlightBounce,
        stiffness = Spring.StiffnessMediumLow
    )

    // ── Tween duration presets ─────────────────────────────────────────────
    val fadeFast = tween<Float>(durationMillis = 150)
    val fadeMedium = tween<Float>(durationMillis = 250)
    val fadeSlow = tween<Float>(durationMillis = 350)
    val fadeXSlow = tween<Float>(durationMillis = 500)

    val moveFast = tween<IntOffset>(durationMillis = 200)
    val moveMedium = tween<IntOffset>(durationMillis = 300)
    val moveSlow = tween<IntOffset>(durationMillis = 450)

    // ── Scale presets ──────────────────────────────────────────────────────
    const val pressScale = 0.97f
    const val cardPressScale = 0.98f
    const val iconPressScale = 0.90f
    const val modalEnterScale = 1.05f
    const val modalExitScale = 0.95f
}
