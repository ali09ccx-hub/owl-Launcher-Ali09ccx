package com.example.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.AppInfo
import com.example.model.IconShapeStyle
import com.example.model.OwlNest
import com.example.ui.LauncherUiState
import com.example.ui.theme.MidnightDeep
import kotlin.math.hypot

private data class ActiveFolderDrag(
    val app: AppInfo,
    val pointerId: PointerId,
    val startPointerInRoot: Offset,
    val currentPointerInRoot: Offset,
    val initialCenterInRoot: Offset,
    val maxTravelPx: Float
) {
    val currentCenterInRoot: Offset
        get() = initialCenterInRoot + (currentPointerInRoot - startPointerInRoot)
}

private enum class FolderDropZone {
    INSIDE_REORDER,
    EXTRACT_TO_HOME,
    REMOVE_FROM_FOLDER
}

@Composable
fun ExpandedFolderOverlay(
    nest: OwlNest,
    uiState: LauncherUiState,
    onDismiss: () -> Unit,
    onRenameNest: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onReorderAppInNest: (String, Int) -> Unit,
    onExtractAppToHome: (String) -> Unit,
    onRemoveAppFromNest: (String) -> Unit,
    onCustomizeNest: () -> Unit,
    onDeleteNest: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val prefs = uiState.prefs
    val apps = nest.appKeys.mapNotNull { uiState.findApp(it) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 4 else 3
    val effectiveShape = nest.shapeOverride ?: prefs.iconShape
    val iconSize = prefs.iconSizeDp.coerceIn(46, 62).dp

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val dragThresholdPx = with(density) { 14.dp.toPx() }
    val removeHitPaddingPx = with(density) { 18.dp.toPx() }

    var editableName by remember(nest.id, nest.name) { mutableStateOf(nest.name) }
    var rootBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var cardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var removeZoneBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var activeDrag by remember { mutableStateOf<ActiveFolderDrag?>(null) }
    val slotBounds = remember(nest.id) { mutableStateMapOf<String, Pair<Int, Rect>>() }

    val latestApps by rememberUpdatedState(apps)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnShowMenu by rememberUpdatedState(onShowAppMenu)
    val latestOnReorder by rememberUpdatedState(onReorderAppInNest)
    val latestOnExtract by rememberUpdatedState(onExtractAppToHome)
    val latestOnRemove by rememberUpdatedState(onRemoveAppFromNest)

    val isMovingOut = activeDrag != null && activeDrag!!.maxTravelPx >= dragThresholdPx
    val activeZone = remember(activeDrag, cardBoundsInRoot, removeZoneBoundsInRoot) {
        val current = activeDrag
        if (current == null || current.maxTravelPx < dragThresholdPx) {
            null
        } else {
            val center = current.currentCenterInRoot
            val inflatedRemove = if (removeZoneBoundsInRoot != Rect.Zero) {
                removeZoneBoundsInRoot.inflate(removeHitPaddingPx)
            } else {
                Rect.Zero
            }
            when {
                inflatedRemove != Rect.Zero && inflatedRemove.contains(center) -> {
                    FolderDropZone.REMOVE_FROM_FOLDER
                }
                cardBoundsInRoot != Rect.Zero && !cardBoundsInRoot.contains(center) -> {
                    FolderDropZone.EXTRACT_TO_HOME
                }
                else -> FolderDropZone.INSIDE_REORDER
            }
        }
    }

    val cardAlpha by animateFloatAsState(
        targetValue = when (activeZone) {
            FolderDropZone.EXTRACT_TO_HOME -> 0.25f
            FolderDropZone.REMOVE_FROM_FOLDER -> 0.45f
            else -> 1f
        },
        label = "folder_card_alpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (activeZone == FolderDropZone.EXTRACT_TO_HOME) 0.92f else 1f,
        label = "folder_card_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightDeep.copy(alpha = 0.72f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onGloballyPositioned { rootBoundsInRoot = it.boundsInRoot() }
            .pointerInput(nest.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val current = activeDrag ?: continue
                        val change = event.changes.firstOrNull { it.id == current.pointerId }
                            ?: event.changes.firstOrNull()
                        if (change != null) {
                            change.consume()
                            val pointerInRoot = rootBoundsInRoot.topLeft + change.position
                            val dist = (pointerInRoot - current.startPointerInRoot).getDistance()
                            val updated = current.copy(
                                currentPointerInRoot = pointerInRoot,
                                maxTravelPx = maxOf(current.maxTravelPx, dist)
                            )
                            activeDrag = updated

                            if (!change.pressed || event.changes.none { it.pressed }) {
                                val savedRemoveBounds = removeZoneBoundsInRoot
                                activeDrag = null
                                removeZoneBoundsInRoot = Rect.Zero

                                if (updated.maxTravelPx < dragThresholdPx) {
                                    latestOnDismiss()
                                    latestOnShowMenu(updated.app)
                                } else {
                                    val center = updated.currentCenterInRoot
                                    val inflatedRemove = if (savedRemoveBounds != Rect.Zero) {
                                        savedRemoveBounds.inflate(removeHitPaddingPx)
                                    } else {
                                        Rect.Zero
                                    }
                                    when {
                                        inflatedRemove != Rect.Zero && inflatedRemove.contains(center) -> {
                                            latestOnRemove(updated.app.componentKey)
                                        }
                                        cardBoundsInRoot != Rect.Zero && !cardBoundsInRoot.contains(center) -> {
                                            latestOnExtract(updated.app.componentKey)
                                        }
                                        else -> {
                                            val nearest = slotBounds.entries.minByOrNull { (_, pair) ->
                                                hypot(
                                                    (center.x - pair.second.center.x).toDouble(),
                                                    (center.y - pair.second.center.y).toDouble()
                                                )
                                            }
                                            val targetIdx = nearest?.value?.first
                                                ?: (latestApps.size - 1).coerceAtLeast(0)
                                            latestOnReorder(updated.app.componentKey, targetIdx)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (activeDrag == null) {
                        focusManager.clearFocus()
                        onDismiss()
                    }
                }
            )
            .testTag("expanded_folder_overlay")
    ) {
        // Top Drop Pill: Remove from folder (visible while dragging an app)
        AnimatedVisibility(
            visible = isMovingOut,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .zIndex(20f)
        ) {
            val isRemoveHovered = activeZone == FolderDropZone.REMOVE_FROM_FOLDER
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isRemoveHovered) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                },
                border = BorderStroke(
                    width = if (isRemoveHovered) 2.dp else 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .onGloballyPositioned { removeZoneBoundsInRoot = it.boundsInRoot() }
                    .testTag("folder_remove_drop_zone")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = if (isRemoveHovered) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.folder_remove_zone),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRemoveHovered) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            Color.White
                        }
                    )
                }
            }
        }

        // Centered Launcher Folder Page Container
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = (prefs.glassOpacity + 0.24f).coerceIn(0.68f, 0.94f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                ),
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 32.dp)
                    .widthIn(max = if (isLandscape) 560.dp else 400.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                    }
                    .onGloballyPositioned { cardBoundsInRoot = it.boundsInRoot() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { focusManager.clearFocus() }
                    )
                    .testTag("expanded_folder_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Actions on side + Editable Folder Title in center
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onCustomizeNest()
                            },
                            modifier = Modifier.testTag("folder_customize_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.folder_customize_desc),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = editableName,
                                onValueChange = { next ->
                                    editableName = next
                                    if (next.isNotBlank()) {
                                        onRenameNest(next)
                                    }
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("folder_inline_title_input")
                            )
                        }

                        IconButton(
                            onClick = onDeleteNest,
                            modifier = Modifier.testTag("folder_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.nest_delete_btn),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (apps.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.nest_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val rows = apps.chunked(columns)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (isLandscape) 240.dp else 390.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = rows,
                                key = { rowIdx, rowApps ->
                                    "folder_row_${rowIdx}_${rowApps.firstOrNull()?.componentKey ?: rowIdx}"
                                }
                            ) { rowIdx, rowApps ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (col in 0 until columns) {
                                        val app = rowApps.getOrNull(col)
                                        val flatIndex = rowIdx * columns + col
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (app != null) {
                                                FolderDraggableAppTile(
                                                    app = app,
                                                    iconShape = effectiveShape,
                                                    iconSize = iconSize,
                                                    showFrameBackdrop = prefs.showIconFrameBackdrop,
                                                    isHiddenWhileDragged = activeDrag?.app?.componentKey == app.componentKey,
                                                    onClick = { onLaunchApp(app) },
                                                    onBoundsChanged = { bounds ->
                                                        slotBounds[app.componentKey] = flatIndex to bounds
                                                    },
                                                    onDisposeSlot = {
                                                        slotBounds.remove(app.componentKey)
                                                    },
                                                    onStartDrag = { pointerId, pointerInRoot, tileBounds ->
                                                        activeDrag = ActiveFolderDrag(
                                                            app = app,
                                                            pointerId = pointerId,
                                                            startPointerInRoot = pointerInRoot,
                                                            currentPointerInRoot = pointerInRoot,
                                                            initialCenterInRoot = tileBounds.center,
                                                            maxTravelPx = 0f
                                                        )
                                                    }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.fillMaxWidth())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Extract-to-Home Hint Pill while dragging outside the folder card
        AnimatedVisibility(
            visible = activeZone == FolderDropZone.EXTRACT_TO_HOME,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .zIndex(20f)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenWith,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.folder_extract_zone),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Physical-LTR Floating Ghost Icon strictly locked to finger across RTL/LTR
        activeDrag?.let { current ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val localCenter = current.currentCenterInRoot - rootBoundsInRoot.topLeft
                    val boxDp = iconSize + 16.dp
                    val boxPx = with(density) { boxDp.toPx() }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .zIndex(50f)
                            .size(boxDp)
                            .graphicsLayer {
                                translationX = localCenter.x - boxPx / 2f
                                translationY = localCenter.y - boxPx / 2f
                                scaleX = 1.18f
                                scaleY = 1.18f
                                alpha = 0.95f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        UnalteredAppIconArtwork(
                            app = current.app,
                            iconShape = effectiveShape,
                            iconSize = iconSize,
                            showFrameBackdrop = prefs.showIconFrameBackdrop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDraggableAppTile(
    app: AppInfo,
    iconShape: IconShapeStyle,
    iconSize: Dp,
    showFrameBackdrop: Boolean,
    isHiddenWhileDragged: Boolean,
    onClick: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    onDisposeSlot: () -> Unit,
    onStartDrag: (PointerId, Offset, Rect) -> Unit
) {
    var tileBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val touchSlopPx = with(density) { 10.dp.toPx() }
    val longPressMs = 300L

    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnStartDrag by rememberUpdatedState(onStartDrag)

    DisposableEffect(app.componentKey) {
        onDispose { onDisposeSlot() }
    }

    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = if (isHiddenWhileDragged) 0f else 1f
            }
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                tileBoundsInRoot = bounds
                onBoundsChanged(bounds)
            }
            .testTag("folder_app_${app.packageName}")
            .clip(RoundedCornerShape(16.dp))
            .indication(interactionSource, ripple())
            .pointerInput(app.componentKey) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture

                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    val downTime = System.currentTimeMillis()
                    var lastLocalPos = down.position
                    var cancelled = false

                    try {
                        while (true) {
                            val elapsed = System.currentTimeMillis() - downTime
                            val remaining = (longPressMs - elapsed).coerceAtLeast(1L)
                            val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }

                            if (event == null) {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (tileBoundsInRoot != Rect.Zero) {
                                    latestOnStartDrag(
                                        down.id,
                                        tileBoundsInRoot.topLeft + lastLocalPos,
                                        tileBoundsInRoot
                                    )
                                }
                                break
                            }

                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                            if (change == null) break

                            lastLocalPos = change.position
                            val dist = (change.position - down.position).getDistance()
                            if (!change.pressed) {
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                if (dist < touchSlopPx) {
                                    change.consume()
                                    latestOnClick()
                                }
                                break
                            } else if (dist > touchSlopPx) {
                                cancelled = true
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                break
                            }
                        }
                    } finally {
                        if (cancelled) {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UnalteredAppIconArtwork(
            app = app,
            iconShape = iconShape,
            iconSize = iconSize,
            showFrameBackdrop = showFrameBackdrop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
