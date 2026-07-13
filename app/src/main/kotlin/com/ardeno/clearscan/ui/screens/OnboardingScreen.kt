package com.ardeno.clearscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ui.components.ClearScanHaptic
import com.ardeno.clearscan.ui.components.rememberClearScanHaptics
import com.ardeno.clearscan.ui.theme.ClearScanMotion
import com.ardeno.clearscan.ui.theme.ClearScanSpacing
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

/** Subtle gradient tint per slide — kept outside composable so the list stays pure data. */
private val slideGradients = listOf(
    Pair(Color(0xFFE3F0FF), Color(0xFFD1E3F8)),
    Pair(Color(0xFFE6F4EA), Color(0xFFCEEAD6)),
    Pair(Color(0xFFFFF3E0), Color(0xFFFCE3B8)),
    Pair(Color(0xFFF3E8FD), Color(0xFFE8D5F5)),
    Pair(Color(0xFFE8EAF6), Color(0xFFD4D8EC)),
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.DocumentScanner,
        title = "Scan privately",
        body = "Capture receipts, contracts, and notes with your camera. Everything stays on this device — no cloud uploads."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Search,
        title = "Search your scans",
        body = "Local OCR makes every document searchable. Find text inside scans instantly, even offline."
    ),
    OnboardingPage(
        icon = Icons.Outlined.CloudOff,
        title = "Free. No ads. No cloud.",
        body = "ClearScan is built to stay free — no subscriptions, no watermarks, and no account required."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Folder,
        title = "Organize with folders and tags",
        body = "Group scans into folders and add smart tags. Keep every document sorted and easy to find."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Lock,
        title = "Privacy vault and secure backup",
        body = "Lock sensitive documents behind biometrics and safeguard your data with encrypted backups."
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val performHaptic = rememberClearScanHaptics()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    // Animated gradient colors that blend between slides
    val gradientStart by animateColorAsState(
        targetValue = slideGradients[pagerState.currentPage].first,
        animationSpec = ClearScanMotion.springStiffColor,
        label = "gradientStart"
    )
    val gradientEnd by animateColorAsState(
        targetValue = slideGradients[pagerState.currentPage].second,
        animationSpec = ClearScanMotion.springStiffColor,
        label = "gradientEnd"
    )

    // Skip-button alpha: 1f on early slides, fades to 0 approaching the last
    val skipAlpha by animateFloatAsState(
        targetValue = if (isLastPage) 0f else 1f,
        animationSpec = ClearScanMotion.fadeSlow,
        label = "skipAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd),
                    start = Offset.Zero,
                    end = Offset(1000f, 1000f)
                )
            )
            .padding(horizontal = ClearScanSpacing.xxl)
    ) {
        // Skip button — fades out on last slide instead of vanishing
        AnimatedVisibility(
            visible = !isLastPage,
            enter = fadeIn(animationSpec = ClearScanMotion.fadeMedium),
            exit = fadeOut(animationSpec = ClearScanMotion.fadeMedium)
        ) {
            TextButton(
                onClick = {
                    performHaptic(ClearScanHaptic.LightTap)
                    onComplete()
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = ClearScanSpacing.lg)
                    .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget)
                    .graphicsLayer(alpha = skipAlpha)
            ) {
                Text("Skip")
            }
        }
        if (isLastPage) {
            Spacer(modifier = Modifier.height(56.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val pageParallaxOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            OnboardingPageContent(
                page = onboardingPages[page],
                parallaxOffset = pageParallaxOffset
            )
        }

        // Slide counter text
        Text(
            text = "${pagerState.currentPage + 1} / ${onboardingPages.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ClearScanSpacing.sm)
        )

        // Page indicator dots with scale + alpha animation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ClearScanSpacing.xxl),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            onboardingPages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val dotSize by animateDpAsState(
                    targetValue = if (selected) 8.dp else 6.dp,
                    animationSpec = ClearScanMotion.springSnappyDp,
                    label = "dotSize"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    },
                    animationSpec = ClearScanMotion.springStiffColor,
                    label = "dotColor"
                )

                // Scale: selected dot pops to 1.3, others sit at 1.0
                val dotScale by animateFloatAsState(
                    targetValue = if (selected) 1.3f else 1.0f,
                    animationSpec = ClearScanMotion.springSnappy,
                    label = "dotScale"
                )
                // Alpha: selected fully opaque, others semi-faded
                val dotAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.45f,
                    animationSpec = ClearScanMotion.fadeMedium,
                    label = "dotAlpha"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = ClearScanSpacing.xs)
                        .size(dotSize)
                        .graphicsLayer(
                            scaleX = dotScale,
                            scaleY = dotScale,
                            alpha = dotAlpha
                        )
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        if (isLastPage) {
            Button(
                onClick = {
                    performHaptic(ClearScanHaptic.Confirm)
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = ClearScanSpacing.xxxl)
                    .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Get Started")
            }
        } else {
            Button(
                onClick = {
                    performHaptic(ClearScanHaptic.Selection)
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = ClearScanSpacing.xxxl)
                    .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    parallaxOffset: Float
) {
    // Parallax: icon container drifts at 0.25× the page-swipe speed.
    // currentPageOffsetFraction returns 0 for the current page,
    // ±1 for immediate neighbours, so this naturally moves icons
    // in the swipe direction.
    val density = LocalDensity.current
    val iconTranslationX = parallaxOffset * with(density) { 24.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClearScanSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer(translationX = iconTranslationX)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(ClearScanSpacing.xxxl))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ClearScanSpacing.md))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = ClearScanSpacing.sm)
        )
    }
}
