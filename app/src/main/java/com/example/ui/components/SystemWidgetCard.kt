package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.IconShapeStyle
import com.example.model.PlacedWidget
import com.example.model.WidgetSize
import com.example.widgets.OwlAppWidgetHost
import kotlin.math.roundToInt

@Composable
fun SystemWidgetCard(
    widget: PlacedWidget,
    widgetHost: OwlAppWidgetHost,
    glassOpacity: Float,
    isEditMode: Boolean,
    onCustomize: () -> Unit,
    onRemove: () -> Unit,
    dragController: HomeDragController? = null,
    dragPayload: HomeDragPayload.Widget? = null,
    pageIndex: Int = 0,
    slotIndex: Int = -1,
    forcedWidthDp: Dp? = null,
    forcedHeightDp: Dp? = null,
    isHiddenWhileDragged: Boolean = false,
    disableHostInteraction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val providerInfo = remember(widget.appWidgetId, widget.providerPackage, widget.providerClass) {
        widgetHost.getProviderInfoForWidgetId(widget.appWidgetId)
            ?: widgetHost.findProviderInfo(widget.providerPackage, widget.providerClass)
    }

    val widgetHeight = forcedHeightDp ?: widget.customHeightDp?.dp ?: when (widget.size) {
        WidgetSize.COMPACT -> 84.dp
        WidgetSize.WIDE -> 124.dp
        WidgetSize.TALL -> 210.dp
    }

    val shape = when (val override = widget.shapeOverride) {
        null, IconShapeStyle.ORIGINAL -> RoundedCornerShape(widget.cornerRadiusDp.dp)
        else -> override.toComposeShape()
    }

    val surfaceColor = if (widget.transparentBackground) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = (glassOpacity * 0.85f).coerceAtLeast(0.22f))
    }

    val borderStroke = when {
        isEditMode -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        widget.transparentBackground -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    }

    var boundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val touchSlopPx = with(density) { 10.dp.toPx() }
    val longPressMs = 360L

    val latestController by rememberUpdatedState(dragController)
    val latestPayload by rememberUpdatedState(dragPayload)
    val latestCustomize by rememberUpdatedState(onCustomize)
    val latestEditMode by rememberUpdatedState(isEditMode)

    if (dragController != null && dragPayload != null) {
        DisposableEffect(dragController, pageIndex, widget.id) {
            onDispose {
                dragController.widgetSlots.remove("${pageIndex}_${widget.id}")
            }
        }
    }

    val dragModifier = if (dragController != null && dragPayload != null) {
        Modifier.pointerInput(widget.id, pageIndex) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val downTime = System.currentTimeMillis()
                var lastLocalPos = down.position
                var consumedByBadge = false

                while (true) {
                    val elapsed = System.currentTimeMillis() - downTime
                    val remaining = (longPressMs - elapsed).coerceAtLeast(1L)
                    val event = withTimeoutOrNull(remaining) {
                        awaitPointerEvent(pass = PointerEventPass.Initial)
                    }

                    if (event == null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val controller = latestController
                        val payload = latestPayload
                        if (controller != null && payload != null && boundsInRoot != Rect.Zero) {
                            controller.startDrag(
                                payload = payload,
                                pointerId = down.id,
                                pointerPosInRoot = boundsInRoot.topLeft + lastLocalPos,
                                tileBoundsInRoot = boundsInRoot
                            )
                        }
                        break
                    }

                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: event.changes.firstOrNull()
                    if (change == null) break
                    lastLocalPos = change.position
                    if (change.isConsumed) {
                        consumedByBadge = true
                    }
                    val dist = (change.position - down.position).getDistance()
                    if (!change.pressed) {
                        if (latestEditMode && !consumedByBadge && dist < touchSlopPx) {
                            change.consume()
                            latestCustomize()
                        }
                        break
                    } else if (dist > touchSlopPx) {
                        break
                    }
                }
            }
        }
    } else {
        Modifier
    }

    val sizeModifier = if (forcedWidthDp != null) {
        modifier
            .width(forcedWidthDp)
            .height(widgetHeight)
    } else if (widget.customWidthDp != null) {
        modifier
            .width(widget.customWidthDp.dp)
            .height(widgetHeight)
    } else {
        modifier
            .fillMaxWidth()
            .height(widgetHeight)
    }

    Box(
        modifier = sizeModifier
            .graphicsLayer {
                alpha = if (isHiddenWhileDragged) 0f else 1f
            }
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                boundsInRoot = bounds
                if (dragController != null && dragPayload != null) {
                    dragController.registerWidgetSlot(
                        widgetId = widget.id,
                        pageIndex = pageIndex,
                        slotIndex = slotIndex,
                        bounds = bounds
                    )
                }
            }
            .then(dragModifier)
            .testTag("system_widget_${widget.appWidgetId}")
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            color = surfaceColor,
            border = borderStroke
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) {
                val measuredWidthDp = maxWidth.value.roundToInt().coerceAtLeast(48)
                val measuredHeightDp = maxHeight.value.roundToInt().coerceAtLeast(48)

                widget.gifPath?.let { gif ->
                    AnimatedGifMedia(
                        filePath = gif,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .graphicsLayer { alpha = 0.28f }
                    )
                }

                if (providerInfo != null && widget.appWidgetId > 0) {
                    AndroidView(
                        factory = {
                            widgetHost.createHostView(widget.appWidgetId, providerInfo).also { view ->
                                widgetHost.updateWidgetSize(view, measuredWidthDp, measuredHeightDp)
                            }
                        },
                        update = { view ->
                            widgetHost.updateWidgetSize(view, measuredWidthDp, measuredHeightDp)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = widget.providerPackage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // In Edit Mode or inside the visual frame editor, absorb clicks so taps open Tune/drag cleanly
                if (isEditMode || disableHostInteraction) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isEditMode && dragController == null) {
                                    Modifier.clickable(onClick = onCustomize)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }

        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRemove)
                    .testTag("widget_remove_btn_${widget.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onCustomize)
                    .testTag("widget_tune_btn_${widget.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

