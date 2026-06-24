package com.ardeno.clearscan.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ui.theme.ClearScanSpacing

@Composable
fun LibraryListSkeleton(
    modifier: Modifier = Modifier,
    rowCount: Int = 4
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ClearScanSpacing.lg, vertical = ClearScanSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
    ) {
        repeat(rowCount) {
            SkeletonDocumentRow()
        }
    }
}

@Composable
fun LibraryGridSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ClearScanSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.lg)
    ) {
        repeat((itemCount + 1) / 2) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
            ) {
                repeat(2) { col ->
                    val index = row * 2 + col
                    if (index < itemCount) {
                        SkeletonGridItem(modifier = Modifier.weight(1f))
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonDocumentRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(ClearScanSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(ClearScanSpacing.md)
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            ShimmerBox(
                modifier = Modifier
                    .size(width = 72.dp, height = 20.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun SkeletonGridItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ClearScanSpacing.sm)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    base,
                    highlight.copy(alpha = 0.6f),
                    base,
                    highlight.copy(alpha = 0.6f),
                    base
                ),
                start = Offset(offset - 300f, 0f),
                end = Offset(offset, 0f)
            )
        )
    )
}
