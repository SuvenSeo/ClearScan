package com.ardeno.clearscan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = ClearScanSpacing.xxl)
    ) {
        if (!isLastPage) {
            TextButton(
                onClick = {
                    performHaptic(ClearScanHaptic.LightTap)
                    onComplete()
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = ClearScanSpacing.lg)
                    .defaultMinSize(minHeight = ClearScanSpacing.minTouchTarget)
            ) {
                Text("Skip")
            }
        } else {
            Spacer(modifier = Modifier.height(56.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPageContent(page = onboardingPages[page])
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
                Box(
                    modifier = Modifier
                        .padding(horizontal = ClearScanSpacing.xs)
                        .size(dotSize)
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
private fun OnboardingPageContent(page: OnboardingPage) {
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
