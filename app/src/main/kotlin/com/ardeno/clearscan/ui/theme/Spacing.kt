package com.ardeno.clearscan.ui.theme

import androidx.compose.ui.unit.dp

/** iOS HIG–aligned spacing and touch targets for ClearScan. */
object ClearScanSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Minimum accessible touch target (Material / iOS). */
    val minTouchTarget = 48.dp

    val screenHorizontal = lg
    val sectionGap = xxl
    val rowVertical = 12.dp
    val groupedInset = lg
}

object ClearScanElevation {
    val none = 0.dp
    val card = 1.dp
    val raised = 2.dp
    val navBar = 3.dp
}
