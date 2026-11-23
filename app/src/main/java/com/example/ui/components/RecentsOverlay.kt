package com.example.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.IconShapeStyle
import com.example.ui.LauncherUiState
import com.example.ui.theme.AmiriEditorialFamily
import com.example.ui.theme.MidnightDeep
import kotlinx.coroutines.launch

private data class ResolvedRecentCard(
    val app: AppInfo,
    val lastOpenedAt: Long
)

@Composable
fun RecentsOverlay(
    uiState: LauncherUiState,
    onDismiss: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onDismissRecent: (String) -> Unit,
    onClearAll: () -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val recentCards = remember(uiState.prefs.recentApps, uiState.installedApps, uiState.prefs.hiddenAppKeys) {
        uiState.prefs.recentApps.mapNotNull { entry ->
            if (entry.componentKey in uiState.prefs.hiddenAppKeys) return@mapNotNull null
            val app = uiState.findApp(entry.componentKey) ?: return@mapNotNull null
            ResolvedRecentCard(app = app, lastOpenedAt = entry.lastOpenedAt)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightDeep.copy(alpha = 0.86f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("recents_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewCarousel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.recents_title),
                        fontFamily = AmiriEditorialFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (recentCards.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.recents_count_format, recentCards.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (recentCards.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAll,
                            modifier = Modifier.testTag("recents_clear_all_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ClearAll,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.recents_clear_all),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("recents_close_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            if (recentCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ViewCarousel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.recents_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.recents_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { recentCards.size }
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val horizontalPeek = if (isLandscape) {
                        (maxWidth * 0.24f).coerceAtLeast(110.dp)
                    } else {
                        (maxWidth * 0.13f).coerceAtLeast(36.dp)
                    }

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = horizontalPeek, vertical = 12.dp),
                        pageSpacing = 16.dp,
                        key = { idx -> recentCards[idx].app.componentKey },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recents_pager")
                    ) { page ->
                        val card = recentCards[page]
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .coerceIn(-1.5f, 1.5f)
                        val scale = 1f - (kotlin.math.abs(pageOffset) * 0.08f).coerceAtMost(0.14f)

                        RecentSnapshotCard(
                            card = card,
                            iconShape = uiState.prefs.iconShape,
                            glassOpacity = uiState.prefs.glassOpacity,
                            isLandscape = isLandscape,
                            onResume = { onLaunchApp(card.app) },
                            onDismissCard = { onDismissRecent(card.app.componentKey) },
                            onOpenAppInfo = { onOpenAppInfo(card.app) },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.recents_swipe_dismiss_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentSnapshotCard(
    card: ResolvedRecentCard,
    iconShape: IconShapeStyle,
    glassOpacity: Float,
    isLandscape: Boolean,
    onResume: () -> Unit,
    onDismissCard: () -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val dragOffsetY = remember(card.app.componentKey) { Animatable(0f) }

    val timeAgoText = formatRelativeTime(card.lastOpenedAt)

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = (glassOpacity + 0.22f).coerceIn(0.68f, 0.96f)),
        border = BorderStroke(
            width = 1.2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
        ),
        tonalElevation = 10.dp,
        modifier = modifier
            .graphicsLayer {
                translationY = dragOffsetY.value
                val dismissProgress = ((-dragOffsetY.value) / (dismissThresholdPx * 2.2f)).coerceIn(0f, 0.78f)
                alpha = 1f - dismissProgress
            }
            .pointerInput(card.app.componentKey) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        val target = (dragOffsetY.value + dragAmount).coerceAtMost(28f)
                        scope.launch {
                            dragOffsetY.snapTo(target)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (dragOffsetY.value <= -dismissThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragOffsetY.animateTo(-dismissThresholdPx * 2.5f)
                                onDismissCard()
                            } else {
                                dragOffsetY.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffsetY.animateTo(0f, spring())
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onResume)
            .testTag("recent_card_${card.app.packageName}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Snapshot Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UnalteredAppIconArtwork(
                    app = card.app,
                    iconShape = iconShape,
                    iconSize = 36.dp,
                    showFrameBackdrop = false
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeAgoText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.action_app_info),
                        tint = Color.White.copy(alpha = 0.78f)
                    )
                }

                IconButton(
                    onClick = onDismissCard,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("recent_dismiss_${card.app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
            }

            // Snapshot Card Visual Preview Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                MidnightDeep.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 86.dp else 112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        UnalteredAppIconArtwork(
                            app = card.app,
                            iconShape = iconShape,
                            iconSize = if (isLandscape) 64.dp else 80.dp,
                            showFrameBackdrop = false
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.recents_tap_to_resume),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun formatRelativeTime(timestampMs: Long): String {
    val diffMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val minutes = (diffMs / 60_000L).toInt()
    val hours = minutes / 60
    return when {
        minutes < 1 -> stringResource(R.string.recents_just_now)
        hours < 1 -> stringResource(R.string.recents_minutes_ago, minutes)
        else -> stringResource(R.string.recents_hours_ago, hours)
    }
}
