package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AppInfo
import com.example.model.FolderSizeMode
import com.example.model.IconShapeStyle
import com.example.model.OwlNest

private val OwlShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    cubicTo(w * 0.88f, 0f, w, h * 0.14f, w, h * 0.44f)
    cubicTo(w, h * 0.75f, w * 0.72f, h * 0.94f, w * 0.5f, h)
    cubicTo(w * 0.28f, h * 0.94f, 0f, h * 0.75f, 0f, h * 0.44f)
    cubicTo(0f, h * 0.14f, w * 0.12f, 0f, w * 0.5f, 0f)
    close()
}

fun IconShapeStyle.toComposeShape(): Shape = when (this) {
    IconShapeStyle.ORIGINAL -> RectangleShape
    IconShapeStyle.SQUIRCLE -> RoundedCornerShape(percent = 30)
    IconShapeStyle.CIRCLE -> CircleShape
    IconShapeStyle.ROUNDED -> RoundedCornerShape(percent = 20)
    IconShapeStyle.FEATHER -> RoundedCornerShape(
        topStartPercent = 48,
        bottomEndPercent = 48,
        topEndPercent = 14,
        bottomStartPercent = 14
    )
    IconShapeStyle.SHIELD -> OwlShieldShape
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconTile(
    app: AppInfo,
    iconShape: IconShapeStyle,
    iconSize: Dp,
    showFrameBackdrop: Boolean,
    showLabel: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCustomizeInEditMode: (() -> Unit)? = null,
    onRemoveInEditMode: (() -> Unit)? = null,
    dragController: HomeDragController? = null,
    dragPayload: HomeDragPayload.App? = null,
    pageIndex: Int = 0,
    indexInPage: Int = 0,
    isMergeHighlighted: Boolean = false,
    isHiddenWhileDragged: Boolean = false,
    modifier: Modifier = Modifier
) {
    var tileBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val touchSlopPx = with(density) { 10.dp.toPx() }
    val longPressMs = 340L

    val latestOnClick by rememberUpdatedState(onClick)
    val latestCustomize by rememberUpdatedState(onCustomizeInEditMode)
    val latestEditMode by rememberUpdatedState(isEditMode)
    val latestPayload by rememberUpdatedState(dragPayload)
    val latestController by rememberUpdatedState(dragController)

    if (dragController != null && dragPayload != null) {
        DisposableEffect(dragController, pageIndex, app.componentKey) {
            onDispose {
                dragController.appSlots.remove("${pageIndex}_${app.componentKey}")
            }
        }
    }

    val mergeScale by animateFloatAsState(
        targetValue = if (isMergeHighlighted) 1.12f else 1f,
        label = "app_merge_scale"
    )

    val interactiveModifier = if (dragController != null && dragPayload != null) {
        Modifier
            .indication(interactionSource, ripple())
            .pointerInput(app.componentKey, pageIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture

                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)

                    val downTime = System.currentTimeMillis()
                    var lastLocalPos = down.position
                    var cancelledByMovement = false
                    var consumedByChildBadge = false

                    try {
                        while (true) {
                            val elapsed = System.currentTimeMillis() - downTime
                            val remaining = (longPressMs - elapsed).coerceAtLeast(1L)
                            val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }

                            if (event == null) {
                                // Long press threshold reached while holding -> hand off to root overlay!
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val controller = latestController
                                val payload = latestPayload
                                if (controller != null && payload != null && tileBoundsInRoot != Rect.Zero) {
                                    val pointerInRoot = tileBoundsInRoot.topLeft + lastLocalPos
                                    controller.startDrag(
                                        payload = payload,
                                        pointerId = down.id,
                                        pointerPosInRoot = pointerInRoot,
                                        tileBoundsInRoot = tileBoundsInRoot
                                    )
                                }
                                break
                            }

                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                            if (change == null) break

                            lastLocalPos = change.position
                            val dist = (change.position - down.position).getDistance()
                            if (change.isConsumed) {
                                consumedByChildBadge = true
                            }
                            if (!change.pressed) {
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                if (!consumedByChildBadge && dist < touchSlopPx) {
                                    change.consume()
                                    if (latestEditMode && latestCustomize != null) {
                                        latestCustomize?.invoke()
                                    } else {
                                        latestOnClick()
                                    }
                                }
                                break
                            } else if (dist > touchSlopPx) {
                                cancelledByMovement = true
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                break
                            }
                        }
                    } finally {
                        if (cancelledByMovement) {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
    } else {
        Modifier.combinedClickable(
            onClick = if (isEditMode && onCustomizeInEditMode != null) onCustomizeInEditMode else onClick,
            onLongClick = onLongClick
        )
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = mergeScale
                scaleY = mergeScale
                alpha = if (isHiddenWhileDragged) 0f else 1f
            }
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                tileBoundsInRoot = bounds
                if (dragController != null && dragPayload != null) {
                    dragController.registerAppSlot(
                        componentKey = app.componentKey,
                        pageIndex = pageIndex,
                        indexInPage = indexInPage,
                        bounds = bounds
                    )
                }
            }
            .testTag("app_icon_${app.packageName}")
            .clip(RoundedCornerShape(16.dp))
            .then(interactiveModifier)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize + 10.dp)
                .then(
                    if (isMergeHighlighted) {
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                )
        ) {
            UnalteredAppIconArtwork(
                app = app,
                iconShape = iconShape,
                iconSize = iconSize,
                showFrameBackdrop = showFrameBackdrop
            )

            if (isEditMode && onRemoveInEditMode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable(onClick = onRemoveInEditMode),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            if (isEditMode && onCustomizeInEditMode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onCustomizeInEditMode),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(5.dp))
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
}

@Composable
fun UnalteredAppIconArtwork(
    app: AppInfo,
    iconShape: IconShapeStyle,
    iconSize: Dp,
    showFrameBackdrop: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = iconShape.toComposeShape()
    val useFrame = showFrameBackdrop && iconShape != IconShapeStyle.ORIGINAL
    val containerModifier = modifier
        .size(iconSize)
        .then(
            if (iconShape != IconShapeStyle.ORIGINAL) {
                Modifier.clip(shape)
            } else {
                Modifier
            }
        )
        .then(
            if (useFrame) {
                Modifier
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.24f),
                        shape = shape
                    )
                    .padding(3.dp)
            } else {
                Modifier
            }
        )

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        val bitmap = app.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (iconShape != IconShapeStyle.ORIGINAL) Modifier.clip(shape) else Modifier)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = app.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize * 0.65f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OwlNestTile(
    nest: OwlNest,
    appsInNest: List<AppInfo>,
    globalIconShape: IconShapeStyle,
    iconSize: Dp,
    showLabel: Boolean,
    glassOpacity: Float,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLaunchAppDirectly: (AppInfo) -> Unit,
    dragController: HomeDragController? = null,
    dragPayload: HomeDragPayload.Nest? = null,
    pageIndex: Int = 0,
    indexInPage: Int = 0,
    isMergeHighlighted: Boolean = false,
    isHiddenWhileDragged: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveShape = nest.shapeOverride ?: globalIconShape
    val shape = if (effectiveShape == IconShapeStyle.ORIGINAL) {
        RoundedCornerShape(20.dp)
    } else {
        effectiveShape.toComposeShape()
    }

    if (dragController != null) {
        DisposableEffect(dragController, pageIndex, nest.id) {
            onDispose {
                dragController.nestSlots.remove("${pageIndex}_${nest.id}")
            }
        }
    }

    val mergeScale by animateFloatAsState(
        targetValue = if (isMergeHighlighted) 1.08f else 1f,
        label = "nest_merge_scale"
    )

    if (nest.folderSize != FolderSizeMode.SIZE_1X1) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = mergeScale
                    scaleY = mergeScale
                }
                .onGloballyPositioned { coords ->
                    if (dragController != null) {
                        dragController.registerNestSlot(
                            nestId = nest.id,
                            pageIndex = pageIndex,
                            indexInPage = indexInPage,
                            bounds = coords.boundsInRoot()
                        )
                    }
                }
                .testTag("owl_nest_big_${nest.id}")
                .clip(RoundedCornerShape(22.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = glassOpacity),
            border = BorderStroke(
                width = if (isMergeHighlighted) 2.dp else 1.dp,
                color = if (isEditMode || isMergeHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                }
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                nest.gifPath?.let { gif ->
                    AnimatedGifMedia(
                        filePath = gif,
                        contentDescription = nest.name,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = 0.35f }
                    )
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = nest.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isEditMode) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val previewCount = if (nest.folderSize == FolderSizeMode.SIZE_2X2) 8 else 4
                    val rows = appsInNest.take(previewCount).chunked(4)
                    if (rows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderSpecial,
                                contentDescription = nest.name,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rows.forEach { rowApps ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowApps.forEach { app ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .combinedClickable(
                                                    onClick = { onLaunchAppDirectly(app) },
                                                    onLongClick = onLongClick
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            UnalteredAppIconArtwork(
                                                app = app,
                                                iconShape = effectiveShape,
                                                iconSize = 40.dp,
                                                showFrameBackdrop = false
                                            )
                                        }
                                    }
                                    repeat((4 - rowApps.size).coerceAtLeast(0)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    var tileBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val touchSlopPx = with(density) { 10.dp.toPx() }
    val longPressMs = 340L

    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnLongClick by rememberUpdatedState(onLongClick)
    val latestEditMode by rememberUpdatedState(isEditMode)
    val latestController by rememberUpdatedState(dragController)
    val latestPayload by rememberUpdatedState(dragPayload)

    val nestInteractiveModifier = if (dragController != null && dragPayload != null) {
        Modifier
            .indication(interactionSource, ripple())
            .pointerInput(nest.id, pageIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture

                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    val downTime = System.currentTimeMillis()
                    var lastLocalPos = down.position
                    var cancelledByMovement = false

                    try {
                        while (true) {
                            val elapsed = System.currentTimeMillis() - downTime
                            val remaining = (longPressMs - elapsed).coerceAtLeast(1L)
                            val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }

                            if (event == null) {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val controller = latestController
                                val payload = latestPayload
                                if (controller != null && payload != null && tileBoundsInRoot != Rect.Zero) {
                                    controller.startDrag(
                                        payload = payload,
                                        pointerId = down.id,
                                        pointerPosInRoot = tileBoundsInRoot.topLeft + lastLocalPos,
                                        tileBoundsInRoot = tileBoundsInRoot
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
                                    if (latestEditMode) latestOnLongClick() else latestOnClick()
                                }
                                break
                            } else if (dist > touchSlopPx) {
                                cancelledByMovement = true
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                break
                            }
                        }
                    } finally {
                        if (cancelledByMovement) {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
    } else {
        Modifier.combinedClickable(
            onClick = if (isEditMode) onLongClick else onClick,
            onLongClick = onLongClick
        )
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = mergeScale
                scaleY = mergeScale
                alpha = if (isHiddenWhileDragged) 0f else 1f
            }
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                tileBoundsInRoot = bounds
                if (dragController != null) {
                    dragController.registerNestSlot(
                        nestId = nest.id,
                        pageIndex = pageIndex,
                        indexInPage = indexInPage,
                        bounds = bounds
                    )
                }
            }
            .testTag("owl_nest_${nest.id}")
            .clip(RoundedCornerShape(16.dp))
            .then(nestInteractiveModifier)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize + 4.dp)
                .clip(shape)
                .background(
                    if (isMergeHighlighted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = glassOpacity)
                    }
                )
                .border(
                    width = if (isMergeHighlighted) 2.dp else 1.dp,
                    color = if (isMergeHighlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                    },
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            val gif = nest.gifPath
            if (gif != null) {
                AnimatedGifMedia(
                    filePath = gif,
                    contentDescription = nest.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (appsInNest.isEmpty()) {
                Icon(
                    imageVector = Icons.Filled.FolderSpecial,
                    contentDescription = nest.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize * 0.5f)
                )
            } else {
                val miniSize = (iconSize - 18.dp) / 2f
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        appsInNest.getOrNull(0)?.let { app ->
                            UnalteredAppIconArtwork(app, IconShapeStyle.ORIGINAL, miniSize, false)
                        }
                        appsInNest.getOrNull(1)?.let { app ->
                            UnalteredAppIconArtwork(app, IconShapeStyle.ORIGINAL, miniSize, false)
                        }
                    }
                    if (appsInNest.size > 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            appsInNest.getOrNull(2)?.let { app ->
                                UnalteredAppIconArtwork(app, IconShapeStyle.ORIGINAL, miniSize, false)
                            }
                            appsInNest.getOrNull(3)?.let { app ->
                                UnalteredAppIconArtwork(app, IconShapeStyle.ORIGINAL, miniSize, false)
                            }
                        }
                    }
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = nest.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
