package com.ardeno.clearscan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// iOS HIG corner radii: ~4pt tight, ~6pt compact, ~10pt grouped rows,
// ~12pt sections, ~16pt cards, ~22pt sheets.
val ClearScanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

// ── Named shape tokens ──────────────────────────────────────────────────────

/** Tightest radius — badges, progress indicators, small adornments. */
val BadgeShape = RoundedCornerShape(4.dp)

/** Chips, compact tags, and status indicators. */
val ChipShape = RoundedCornerShape(8.dp)

/** Search bar and compact input fields. */
val SearchFieldShape = RoundedCornerShape(10.dp)

/** Inset grouped list section (Settings-style). */
val GroupedSectionShape = RoundedCornerShape(12.dp)

/** Document cards and sheet surfaces. */
val CardShape = RoundedCornerShape(16.dp)

/** Dialog and alert surfaces. */
val DialogShape = RoundedCornerShape(14.dp)

/** Bottom sheet top corners. */
val SheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

/** Pill buttons, chips, and segmented controls (fully rounded). */
val PillShape = RoundedCornerShape(50)

/** Icon button / small circular hit target. */
val IconButtonShape = RoundedCornerShape(10.dp)

/** Navigation bar / tab bar shape. */
val NavBarShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
