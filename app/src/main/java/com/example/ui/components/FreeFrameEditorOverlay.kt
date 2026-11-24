package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.IconShapeStyle
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
import com.example.model.PlacedWidget
import com.example.model.PlacedWidgetKind
import com.example.ui.ItemCustomizationTarget
import com.example.ui.LauncherUiState
import com.example.widgets.OwlAppWidgetHost
import kotlin.math.roundToInt

/**
 * Dedicated visual "التعديل الأفضل" frame editor with draggable white edge & corner lines
 * for Apps, Folders (OwlNests), and Widgets — enabling free rectangle/square/tall/wide resizing
 * plus Circle, Triangle, Square, and custom icon shapes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FreeFrameEditorOverlay(
    target: ItemCustomizationTarget,
    uiState: LauncherUiState,
    widgetHost: OwlAppWidgetHost,
    onDismiss: () -> Unit,
    onSaveApp: (PlacedAppItem) -> Unit,
    onSaveNest: (OwlNest) -> Unit,
    onSaveWidget: (PlacedWidget) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.prefs
    val baseIconDp = prefs.iconSizeDp.toFloat().coerceIn(40f, 72f)
    val density = LocalDensity.current

    // Initial dimensions in Dp + shapeOverride + widget transparency
    val initialWidthDp = when (target) {
        is ItemCustomizationTarget.AppTarget -> (baseIconDp * target.item.effectiveWidthScale).coerceIn(36f, 240f)
        is ItemCustomizationTarget.NestTarget -> (baseIconDp * target.nest.widthScale).coerceIn(40f, 260f)
        is ItemCustomizationTarget.WidgetTarget -> (target.widget.customWidthDp ?: 220).toFloat().coerceIn(52f, 340f)
    }
    val initialHeightDp = when (target) {
        is ItemCustomizationTarget.AppTarget -> (baseIconDp * target.item.effectiveHeightScale).coerceIn(36f, 240f)
        is ItemCustomizationTarget.NestTarget -> (baseIconDp * target.nest.heightScale).coerceIn(40f, 260f)
        is ItemCustomizationTarget.WidgetTarget -> (target.widget.customHeightDp ?: 124).toFloat().coerceIn(52f, 340f)
    }
    val initialShape = when (target) {
        is ItemCustomizationTarget.AppTarget -> target.item.shapeOverride
        is ItemCustomizationTarget.NestTarget -> target.nest.shapeOverride
        is ItemCustomizationTarget.WidgetTarget -> target.widget.shapeOverride
    }
    val initialTransparent = when (target) {
        is ItemCustomizationTarget.WidgetTarget -> target.widget.transparentBackground
        else -> true
    }

    var widthDp by remember(target) { mutableFloatStateOf(initialWidthDp) }
    var heightDp by remember(target) { mutableFloatStateOf(initialHeightDp) }
    var selectedShape by remember(target) { mutableStateOf(initialShape) }
    var transparentBg by remember(target) { mutableStateOf(initialTransparent) }
    var cornerRadiusDp by remember(target) {
        mutableIntStateOf((target as? ItemCustomizationTarget.WidgetTarget)?.widget?.cornerRadiusDp ?: 20)
    }

    val minDim = if (target is ItemCustomizationTarget.WidgetTarget) 52f else 36f
    val maxDimW = if (target is ItemCustomizationTarget.WidgetTarget) 330f else 240f
    val maxDimH = if (target is ItemCustomizationTarget.WidgetTarget) 340f else 240f

    Surface(
        color = Color(0xEC080D16),
        modifier = modifier
            .fillMaxSize()
            .testTag("free_frame_editor_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.better_editor_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        when (target) {
                            is ItemCustomizationTarget.AppTarget -> {
                                val wScale = (widthDp / baseIconDp).coerceIn(0.55f, 3.6f)
                                val hScale = (heightDp / baseIconDp).coerceIn(0.55f, 3.6f)
                                onSaveApp(
                                    target.item.copy(
                                        scale = ((wScale + hScale) / 2f).coerceIn(0.6f, 2.8f),
                                        widthScale = wScale,
                                        heightScale = hScale,
                                        shapeOverride = selectedShape
                                    )
                                )
                            }
                            is ItemCustomizationTarget.NestTarget -> {
                                val wScale = (widthDp / baseIconDp).coerceIn(0.55f, 3.6f)
                                val hScale = (heightDp / baseIconDp).coerceIn(0.55f, 3.6f)
                                onSaveNest(
                                    target.nest.copy(
                                        widthScale = wScale,
                                        heightScale = hScale,
                                        shapeOverride = selectedShape
                                    )
                                )
                            }
                            is ItemCustomizationTarget.WidgetTarget -> {
                                val finalW = widthDp.roundToInt().coerceIn(52, 340)
                                val finalH = heightDp.roundToInt().coerceIn(52, 340)
                                onSaveWidget(
                                    target.widget.copy(
                                        customWidthDp = finalW,
                                        customHeightDp = finalH,
                                        shapeOverride = selectedShape,
                                        transparentBackground = transparentBg,
                                        cornerRadiusDp = cornerRadiusDp
                                    )
                                )
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.testTag("better_editor_save_btn")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.save))
                }
            }

            Text(
                text = stringResource(R.string.better_editor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            // Interactive White-Line Resize Canvas (Physical LTR for handles)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle grid background cues
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            pathEffect = dash
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(0f, cy),
                            end = Offset(size.width, cy),
                            pathEffect = dash
                        )
                    }

                    val frameW: Dp = widthDp.dp
                    val frameH: Dp = heightDp.dp
                    val handlePadding = 22.dp

                    Box(
                        modifier = Modifier.size(width = frameW + handlePadding * 2, height = frameH + handlePadding * 2),
                        contentAlignment = Alignment.Center
                    ) {
                        // Live Item Preview inside the exact white frame bounds
                        Box(
                            modifier = Modifier.size(width = frameW, height = frameH),
                            contentAlignment = Alignment.Center
                        ) {
                            when (target) {
                                is ItemCustomizationTarget.AppTarget -> {
                                    UnalteredAppIconArtwork(
                                        app = target.app,
                                        iconShape = selectedShape ?: prefs.iconShape,
                                        iconSize = baseIconDp.dp,
                                        iconWidth = frameW,
                                        iconHeight = frameH,
                                        showFrameBackdrop = prefs.showIconFrameBackdrop
                                    )
                                }

                                is ItemCustomizationTarget.NestTarget -> {
                                    val appsInNest = target.nest.appKeys.mapNotNull { uiState.findApp(it) }
                                    val effShape = selectedShape ?: prefs.iconShape
                                    val shape = if (effShape == IconShapeStyle.ORIGINAL) {
                                        RoundedCornerShape(20.dp)
                                    } else {
                                        effShape.toComposeShape()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(shape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = prefs.glassOpacity))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                                shape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val miniSize = ((minOf(frameW, frameH) - 18.dp) / 2f).coerceAtLeast(12.dp)
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                appsInNest.getOrNull(0)?.let {
                                                    UnalteredAppIconArtwork(it, IconShapeStyle.ORIGINAL, miniSize, false)
                                                }
                                                appsInNest.getOrNull(1)?.let {
                                                    UnalteredAppIconArtwork(it, IconShapeStyle.ORIGINAL, miniSize, false)
                                                }
                                            }
                                            if (appsInNest.size > 2) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    appsInNest.getOrNull(2)?.let {
                                                        UnalteredAppIconArtwork(it, IconShapeStyle.ORIGINAL, miniSize, false)
                                                    }
                                                    appsInNest.getOrNull(3)?.let {
                                                        UnalteredAppIconArtwork(it, IconShapeStyle.ORIGINAL, miniSize, false)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                is ItemCustomizationTarget.WidgetTarget -> {
                                    val previewWidget = target.widget.copy(
                                        customWidthDp = widthDp.roundToInt(),
                                        customHeightDp = heightDp.roundToInt(),
                                        shapeOverride = selectedShape,
                                        transparentBackground = transparentBg,
                                        cornerRadiusDp = cornerRadiusDp
                                    )
                                    when (previewWidget.kind) {
                                        PlacedWidgetKind.ANDROID_APPWIDGET -> {
                                            SystemWidgetCard(
                                                widget = previewWidget,
                                                widgetHost = widgetHost,
                                                glassOpacity = prefs.glassOpacity,
                                                isEditMode = false,
                                                onCustomize = {},
                                                onRemove = {},
                                                forcedWidthDp = frameW,
                                                forcedHeightDp = frameH,
                                                disableHostInteraction = true
                                            )
                                        }
                                        PlacedWidgetKind.OWL_NATIVE -> {
                                            OwlNativeWidgetCard(
                                                widget = previewWidget,
                                                glassOpacity = prefs.glassOpacity,
                                                deviceStatus = uiState.deviceStatus,
                                                focusSecondsRemaining = uiState.focusSecondsRemaining,
                                                focusTimerRunning = uiState.focusTimerRunning,
                                                dispatchNoteText = prefs.dispatchNoteText,
                                                wisdomIndex = uiState.wisdomIndex,
                                                isEditMode = false,
                                                onToggleFocusTimer = {},
                                                onResetFocusTimer = {},
                                                onToggleFlashlight = {},
                                                onOpenNoteEditor = {},
                                                onCycleWisdom = {},
                                                onPickGifForWidget = {},
                                                onCustomize = {},
                                                onRemove = {},
                                                forcedWidthDp = frameW,
                                                forcedHeightDp = frameH
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Crisp White Frame Border Lines directly wrapping the item
                        Canvas(
                            modifier = Modifier.size(width = frameW, height = frameH)
                        ) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset.Zero,
                                size = Size(size.width, size.height),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Top White Horizontal Line Handle (drag up/down to stretch/shrink height)
                        WhiteEdgeHandle(
                            horizontal = true,
                            modifier = Modifier.align(Alignment.TopCenter),
                            onDragDelta = { _, dyPx ->
                                val dyDp = with(density) { dyPx.toDp().value }
                                heightDp = (heightDp - dyDp * 2f).coerceIn(minDim, maxDimH)
                            }
                        )

                        // Bottom White Horizontal Line Handle
                        WhiteEdgeHandle(
                            horizontal = true,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onDragDelta = { _, dyPx ->
                                val dyDp = with(density) { dyPx.toDp().value }
                                heightDp = (heightDp + dyDp * 2f).coerceIn(minDim, maxDimH)
                            }
                        )

                        // Left White Vertical Line Handle (drag left/right to stretch/shrink width)
                        WhiteEdgeHandle(
                            horizontal = false,
                            modifier = Modifier.align(Alignment.CenterStart),
                            onDragDelta = { dxPx, _ ->
                                val dxDp = with(density) { dxPx.toDp().value }
                                widthDp = (widthDp - dxDp * 2f).coerceIn(minDim, maxDimW)
                            }
                        )

                        // Right White Vertical Line Handle
                        WhiteEdgeHandle(
                            horizontal = false,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onDragDelta = { dxPx, _ ->
                                val dxDp = with(density) { dxPx.toDp().value }
                                widthDp = (widthDp + dxDp * 2f).coerceIn(minDim, maxDimW)
                            }
                        )

                        // 4 White Corner Handles (diagonal resize of width & height simultaneously)
                        WhiteCornerHandle(
                            modifier = Modifier.align(Alignment.TopStart),
                            onDragDelta = { dxPx, dyPx ->
                                val dxDp = with(density) { dxPx.toDp().value }
                                val dyDp = with(density) { dyPx.toDp().value }
                                widthDp = (widthDp - dxDp * 2f).coerceIn(minDim, maxDimW)
                                heightDp = (heightDp - dyDp * 2f).coerceIn(minDim, maxDimH)
                            }
                        )
                        WhiteCornerHandle(
                            modifier = Modifier.align(Alignment.TopEnd),
                            onDragDelta = { dxPx, dyPx ->
                                val dxDp = with(density) { dxPx.toDp().value }
                                val dyDp = with(density) { dyPx.toDp().value }
                                widthDp = (widthDp + dxDp * 2f).coerceIn(minDim, maxDimW)
                                heightDp = (heightDp - dyDp * 2f).coerceIn(minDim, maxDimH)
                            }
                        )
                        WhiteCornerHandle(
                            modifier = Modifier.align(Alignment.BottomStart),
                            onDragDelta = { dxPx, dyPx ->
                                val dxDp = with(density) { dxPx.toDp().value }
                                val dyDp = with(density) { dyPx.toDp().value }
                                widthDp = (widthDp - dxDp * 2f).coerceIn(minDim, maxDimW)
                                heightDp = (heightDp + dyDp * 2f).coerceIn(minDim, maxDimH)
                            }
                        )
                        WhiteCornerHandle(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            onDragDelta = { dxPx, dyPx ->
                                val dxDp = with(density) { dxPx.toDp().value }
                                val dyDp = with(density) { dyPx.toDp().value }
                                widthDp = (widthDp + dxDp * 2f).coerceIn(minDim, maxDimW)
                                heightDp = (heightDp + dyDp * 2f).coerceIn(minDim, maxDimH)
                            }
                        )
                    }

                    // Live size indicator badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${widthDp.roundToInt()} × ${heightDp.roundToInt()} dp",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls below canvas (Quick Presets, Shapes including Circle & Triangle, Transparent toggle)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Presets Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isWidget = target is ItemCustomizationTarget.WidgetTarget
                    PresetButton(stringResource(R.string.preset_small_square)) {
                        if (isWidget) {
                            widthDp = 76f
                            heightDp = 76f
                        } else {
                            widthDp = baseIconDp * 0.85f
                            heightDp = baseIconDp * 0.85f
                        }
                    }
                    PresetButton(stringResource(R.string.preset_medium_square)) {
                        if (isWidget) {
                            widthDp = 148f
                            heightDp = 148f
                        } else {
                            widthDp = baseIconDp * 1.3f
                            heightDp = baseIconDp * 1.3f
                        }
                    }
                    PresetButton(stringResource(R.string.preset_wide_rect)) {
                        if (isWidget) {
                            widthDp = 276f
                            heightDp = 100f
                        } else {
                            widthDp = (baseIconDp * 2.1f).coerceAtMost(maxDimW)
                            heightDp = baseIconDp
                        }
                    }
                    PresetButton(stringResource(R.string.preset_tall_rect)) {
                        if (isWidget) {
                            widthDp = 120f
                            heightDp = 230f
                        } else {
                            widthDp = baseIconDp
                            heightDp = (baseIconDp * 2.1f).coerceAtMost(maxDimH)
                        }
                    }
                    PresetButton(stringResource(R.string.preset_reset)) {
                        if (isWidget) {
                            widthDp = 220f
                            heightDp = 120f
                        } else {
                            widthDp = baseIconDp
                            heightDp = baseIconDp
                        }
                    }
                }

                // Shapes (including Square, Circle, Triangle, Squircle, Rounded, Feather, Shield)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedShape == null,
                        onClick = { selectedShape = null },
                        label = { Text(stringResource(R.string.shape_inherit)) }
                    )
                    listOf(
                        IconShapeStyle.ORIGINAL to R.string.shape_original,
                        IconShapeStyle.SQUARE to R.string.shape_square,
                        IconShapeStyle.CIRCLE to R.string.shape_circle,
                        IconShapeStyle.TRIANGLE to R.string.shape_triangle,
                        IconShapeStyle.SQUIRCLE to R.string.shape_squircle,
                        IconShapeStyle.ROUNDED to R.string.shape_rounded,
                        IconShapeStyle.FEATHER to R.string.shape_feather,
                        IconShapeStyle.SHIELD to R.string.shape_shield
                    ).forEach { (shape, resId) ->
                        FilterChip(
                            selected = selectedShape == shape,
                            onClick = { selectedShape = shape },
                            label = { Text(stringResource(resId)) }
                        )
                    }
                }

                if (target is ItemCustomizationTarget.WidgetTarget) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { transparentBg = !transparentBg }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.widget_transparent_bg_toggle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Switch(
                            checked = transparentBg,
                            onCheckedChange = { transparentBg = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteEdgeHandle(
    horizontal: Boolean,
    onDragDelta: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .pointerInput(horizontal) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (horizontal) 34.dp else 6.dp,
                    height = if (horizontal) 6.dp else 34.dp
                )
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.45f), CircleShape)
        )
    }
}

@Composable
private fun WhiteCornerHandle(
    onDragDelta: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, Color.Black.copy(alpha = 0.55f), CircleShape)
        )
    }
}

@Composable
private fun PresetButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}
