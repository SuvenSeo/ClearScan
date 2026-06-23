package com.ardeno.clearscan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// iOS HIG corner radii: ~10pt grouped rows, ~12pt sections, ~16pt cards
val ClearScanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

/** Inset grouped list section (Settings-style) */
val GroupedSectionShape = RoundedCornerShape(12.dp)

/** Search bar and compact input fields */
val SearchFieldShape = RoundedCornerShape(10.dp)

/** Document cards and sheet surfaces */
val CardShape = RoundedCornerShape(16.dp)

/** Pill buttons, chips, and segmented controls */
val PillShape = RoundedCornerShape(50)

/** Bottom sheet top corners */
val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
