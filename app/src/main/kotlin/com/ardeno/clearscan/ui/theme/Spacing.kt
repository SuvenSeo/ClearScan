package com.ardeno.clearscan.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS HIG–aligned spacing for ClearScan.
 *
 * Follows an 8dp grid with intermediate 2dp steps where needed.
 * Minimum touch target is 48dp (Material / iOS HIG).
 */
object ClearScanSpacing {
    // ── Core spacing scale (4dp–48dp) ──────────────────────────────────────
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val smd = 10.dp
    val md = 12.dp
    val mlg = 14.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val xxxxl = 40.dp
    val xxxxxl = 48.dp

    // ── Semantic spacing (aliases for readability) ─────────────────────────

    /** Standard screen horizontal padding. */
    val screenHorizontal = lg

    /** Gap between major sections (e.g. between grouped list sections). */
    val sectionGap = xxl

    /** Vertical spacing inside a row / list item. */
    val rowVertical = md

    /** Horizontal inset for grouped list sections. */
    val groupedInset = lg

    /** Spacing between grid items. */
    val gridSpacing = sm

    /** Spacing between stacked cards. */
    val cardSpacing = md

    /** Gap between an icon and adjacent text. */
    val iconTextGap = smd

    /** Horizontal padding inside a chip / badge. */
    val chipHorizontal = smd

    /** Vertical padding inside a chip / badge. */
    val chipVertical = xxs

    /** Padding inside a grouped row. */
    val rowHorizontal = lg

    /** Minimum accessible touch target dimension. */
    val minTouchTarget = 48.dp
}

object ClearScanElevation {
    val none = 0.dp
    val card = 1.dp
    val raised = 2.dp
    val navBar = 3.dp
    val modal = 8.dp
}
