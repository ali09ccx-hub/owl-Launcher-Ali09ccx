package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceStatus
import com.example.model.IconShapeStyle
import com.example.model.OwlNativeWidgetType
import com.example.model.PlacedWidget
import com.example.model.WidgetSize
import com.example.ui.theme.AmiriEditorialFamily
import com.example.ui.theme.SpaceGroteskFamily
import java.util.Locale

@Composable
fun OwlNativeWidgetCard(
    widget: PlacedWidget,
    glassOpacity: Float,
    deviceStatus: DeviceStatus,
    focusSecondsRemaining: Int,
    focusTimerRunning: Boolean,
    dispatchNoteText: String,
    wisdomIndex: Int,
    isEditMode: Boolean,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onCycleWisdom: () -> Unit,
    onPickGifForWidget: (android.net.Uri) -> Unit,
    onCustomize: () -> Unit,
    onRemove: () -> Unit,
    dragController: HomeDragController? = null,
    dragPayload: HomeDragPayload.Widget? = null,
    pageIndex: Int = 0,
    slotIndex: Int = -1,
    forcedWidthDp: Dp? = null,
    forcedHeightDp: Dp? = null,
    isHiddenWhileDragged: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gifPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPickGifForWidget(it) }
    }

    val targetHeight = forcedHeightDp ?: widget.customHeightDp?.dp ?: when (widget.size) {
        WidgetSize.COMPACT -> 84.dp
        WidgetSize.WIDE -> if (widget.nativeType == OwlNativeWidgetType.GIF_STICKER) 148.dp else 96.dp
        WidgetSize.TALL -> 184.dp
    }

    val shape = when (val override = widget.shapeOverride) {
        null, IconShapeStyle.ORIGINAL -> RoundedCornerShape(widget.cornerRadiusDp.dp)
        else -> override.toComposeShape()
    }

    val surfaceColor = if (widget.transparentBackground) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = glassOpacity)
    }

    val borderStroke = when {
        isEditMode -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        widget.transparentBackground -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
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
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
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
                    if (change.isConsumed) consumedByBadge = true
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
            .height(targetHeight)
    } else if (widget.customWidthDp != null) {
        modifier
            .width(widget.customWidthDp.dp)
            .height(targetHeight)
    } else {
        modifier
            .fillMaxWidth()
            .heightIn(min = targetHeight)
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
            .testTag("owl_widget_${widget.id}")
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            color = surfaceColor,
            border = borderStroke,
            tonalElevation = if (widget.transparentBackground) 0.dp else 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) {
                if (widget.gifPath != null && widget.nativeType != OwlNativeWidgetType.GIF_STICKER) {
                    AnimatedGifMedia(
                        filePath = widget.gifPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(shape)
                            .graphicsLayer { alpha = 0.28f }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    when (widget.nativeType) {
                        OwlNativeWidgetType.GIF_STICKER -> {
                            val gifPath = widget.gifPath
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(shape)
                                    .clickable {
                                        gifPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            if (gifPath != null) {
                                AnimatedGifMedia(
                                    filePath = gifPath,
                                    contentDescription = stringResource(R.string.owl_widget_gif_title),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.GifBox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.gif_tap_to_pick),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OwlNativeWidgetType.FOCUS_TIMER -> {
                        val mins = focusSecondsRemaining / 60
                        val secs = focusSecondsRemaining % 60
                        val timeText = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.owl_widget_focus_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = timeText,
                                    fontFamily = SpaceGroteskFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = onToggleFocusTimer,
                                    modifier = Modifier.testTag("focus_timer_toggle_btn")
                                ) {
                                    Icon(
                                        imageVector = if (focusTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(
                                            if (focusTimerRunning) R.string.focus_pause else R.string.focus_start
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = onResetFocusTimer,
                                    modifier = Modifier.testTag("focus_timer_reset_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = stringResource(R.string.focus_reset)
                                    )
                                }
                            }
                        }
                    }

                    OwlNativeWidgetType.DISPATCH_NOTE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenNoteEditor)
                                .testTag("dispatch_note_widget")
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.owl_widget_note_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dispatchNoteText.ifBlank {
                                    stringResource(R.string.note_placeholder)
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = AmiriEditorialFamily
                                ),
                                color = if (dispatchNoteText.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = if (widget.size == WidgetSize.TALL) 6 else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OwlNativeWidgetType.SYSTEM_GLANCE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (deviceStatus.isCharging) {
                                        Icons.Filled.BatteryChargingFull
                                    } else {
                                        Icons.Filled.BatteryStd
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        if (deviceStatus.isCharging) R.string.battery_charging_format else R.string.battery_format,
                                        deviceStatus.batteryPercent
                                    ),
                                    fontFamily = SpaceGroteskFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        R.string.storage_free_format,
                                        deviceStatus.freeStorageText.ifBlank { "--" }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = onToggleFlashlight,
                                modifier = Modifier.testTag("widget_torch_btn")
                            ) {
                                Icon(
                                    imageVector = if (deviceStatus.torchEnabled) {
                                        Icons.Filled.FlashlightOn
                                    } else {
                                        Icons.Filled.FlashlightOff
                                    },
                                    contentDescription = stringResource(R.string.flashlight_label),
                                    tint = if (deviceStatus.torchEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.flashlight_label))
                            }
                        }
                    }

                    OwlNativeWidgetType.OWL_WISDOM, null -> {
                        val wisdomResIds = listOf(
                            R.string.wisdom_1,
                            R.string.wisdom_2,
                            R.string.wisdom_3,
                            R.string.wisdom_4,
                            R.string.wisdom_5
                        )
                        val quote = stringResource(wisdomResIds[wisdomIndex % wisdomResIds.size])
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onCycleWisdom)
                                .testTag("owl_wisdom_widget")
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = quote,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = AmiriEditorialFamily,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (widget.size == WidgetSize.TALL) 5 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
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
                    .clickable(onClick = onRemove),
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
                    .clickable(onClick = onCustomize),
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

@Composable
fun WidgetEditHeader(
    size: WidgetSize,
    onCustomize: () -> Unit,
    onCycleSize: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val sizeText = when (size) {
        WidgetSize.COMPACT -> stringResource(R.string.widget_size_compact)
        WidgetSize.WIDE -> stringResource(R.string.widget_size_wide)
        WidgetSize.TALL -> stringResource(R.string.widget_size_tall)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                onClick = onCycleSize,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AspectRatio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Surface(
                onClick = onCustomize,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.widget_remove),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
