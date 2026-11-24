package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.AppInfo
import com.example.model.IconShapeStyle
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
import com.example.model.PlacedWidget
import kotlinx.coroutines.delay
import kotlin.math.hypot

sealed interface HomeGridEntry {
    data class NestEntry(val nest: OwlNest) : HomeGridEntry
    data class AppEntry(val placed: PlacedAppItem, val app: AppInfo) : HomeGridEntry
}

data class HomeGridAllocation(
    val slotMap: Map<Int, HomeGridEntry>,
    val slotIndexByAppKey: Map<String, Int>,
    val slotIndexByNestId: Map<String, Int>,
    val totalRows: Int
)

fun allocateHomeGridSlots(
    smallNests: List<OwlNest>,
    pageApps: List<Pair<PlacedAppItem, AppInfo>>,
    columns: Int,
    minRows: Int,
    isLandscape: Boolean
): HomeGridAllocation {
    val occupied = linkedMapOf<Int, HomeGridEntry>()
    val nestSlotById = mutableMapOf<String, Int>()
    val appSlotByKey = mutableMapOf<String, Int>()
    val unassigned = mutableListOf<Pair<HomeGridEntry, (Int) -> Unit>>()

    smallNests.forEach { nest ->
        val preferred = if (isLandscape) nest.landscapeSlotIndex else nest.slotIndex
        if (preferred >= 0 && !occupied.containsKey(preferred)) {
            occupied[preferred] = HomeGridEntry.NestEntry(nest)
            nestSlotById[nest.id] = preferred
        } else {
            unassigned.add(HomeGridEntry.NestEntry(nest) to { slot -> nestSlotById[nest.id] = slot })
        }
    }

    pageApps.forEach { (placed, app) ->
        val preferred = if (isLandscape) placed.landscapeSlotIndex else placed.slotIndex
        if (preferred >= 0 && !occupied.containsKey(preferred)) {
            occupied[preferred] = HomeGridEntry.AppEntry(placed, app)
            appSlotByKey[app.componentKey] = preferred
        } else {
            unassigned.add(
                HomeGridEntry.AppEntry(placed, app) to { slot -> appSlotByKey[app.componentKey] = slot }
            )
        }
    }

    var cursor = 0
    for ((entry, onClaimed) in unassigned) {
        while (occupied.containsKey(cursor)) {
            cursor++
        }
        occupied[cursor] = entry
        onClaimed(cursor)
        cursor++
    }

    val maxSlot = occupied.keys.maxOrNull() ?: -1
    val requiredRows = if (maxSlot < 0) 0 else (maxSlot / columns) + 1
    val totalRows = maxOf(minRows, requiredRows)

    return HomeGridAllocation(
        slotMap = occupied,
        slotIndexByAppKey = appSlotByKey,
        slotIndexByNestId = nestSlotById,
        totalRows = totalRows
    )
}

sealed interface HomeDragPayload {
    data class App(
        val app: AppInfo,
        val placed: PlacedAppItem,
        val sourcePage: Int,
        val iconShape: IconShapeStyle,
        val iconSize: Dp,
        val iconWidth: Dp = iconSize,
        val iconHeight: Dp = iconSize,
        val showFrameBackdrop: Boolean
    ) : HomeDragPayload

    data class Nest(
        val nest: OwlNest,
        val appsInNest: List<AppInfo>,
        val sourcePage: Int,
        val iconShape: IconShapeStyle,
        val iconSize: Dp,
        val glassOpacity: Float
    ) : HomeDragPayload

    data class Widget(
        val widget: PlacedWidget,
        val sourcePage: Int,
        val previewWidthDp: Dp,
        val previewHeightDp: Dp
    ) : HomeDragPayload
}

sealed interface HomeDropTarget {
    data class MergeAppsIntoFolder(val targetAppKey: String) : HomeDropTarget
    data class AddToExistingNest(val targetNestId: String) : HomeDropTarget
    data class MoveAppToSlot(val targetPage: Int, val targetSlotIndex: Int) : HomeDropTarget
    data class MoveNestToSlot(val targetPage: Int, val targetSlotIndex: Int) : HomeDropTarget
    data class MoveWidgetToSlot(val targetPage: Int, val targetSlotIndex: Int) : HomeDropTarget
}

data class RegisteredAppSlot(
    val componentKey: String,
    val pageIndex: Int,
    val indexInPage: Int,
    val boundsInRoot: Rect
)

data class RegisteredNestSlot(
    val nestId: String,
    val pageIndex: Int,
    val indexInPage: Int,
    val boundsInRoot: Rect
)

data class RegisteredWidgetSlot(
    val widgetId: String,
    val pageIndex: Int,
    val slotIndex: Int,
    val boundsInRoot: Rect
)

data class RegisteredEmptySlot(
    val pageIndex: Int,
    val slotIndex: Int,
    val boundsInRoot: Rect
)

data class ActiveHomeDrag(
    val payload: HomeDragPayload,
    val pointerId: PointerId,
    val startPointerInRoot: Offset,
    val currentPointerInRoot: Offset,
    val initialCenterInRoot: Offset,
    val maxTravelPx: Float
) {
    val dragDelta: Offset get() = currentPointerInRoot - startPointerInRoot
    val currentCenterInRoot: Offset get() = initialCenterInRoot + dragDelta
}

@Stable
class HomeDragController {
    var containerBoundsInRoot by mutableStateOf(Rect.Zero)
    var activeDrag by mutableStateOf<ActiveHomeDrag?>(null)
        private set

    val appSlots = mutableStateMapOf<String, RegisteredAppSlot>()
    val nestSlots = mutableStateMapOf<String, RegisteredNestSlot>()
    val widgetSlots = mutableStateMapOf<String, RegisteredWidgetSlot>()
    val emptySlots = mutableStateMapOf<String, RegisteredEmptySlot>()

    val isDragging: Boolean get() = activeDrag != null

    fun registerAppSlot(
        componentKey: String,
        pageIndex: Int,
        indexInPage: Int,
        bounds: Rect
    ) {
        appSlots["${pageIndex}_$componentKey"] = RegisteredAppSlot(
            componentKey = componentKey,
            pageIndex = pageIndex,
            indexInPage = indexInPage,
            boundsInRoot = bounds
        )
    }

    fun registerNestSlot(
        nestId: String,
        pageIndex: Int,
        indexInPage: Int,
        bounds: Rect
    ) {
        nestSlots["${pageIndex}_$nestId"] = RegisteredNestSlot(
            nestId = nestId,
            pageIndex = pageIndex,
            indexInPage = indexInPage,
            boundsInRoot = bounds
        )
    }

    fun registerWidgetSlot(
        widgetId: String,
        pageIndex: Int,
        slotIndex: Int,
        bounds: Rect
    ) {
        widgetSlots["${pageIndex}_$widgetId"] = RegisteredWidgetSlot(
            widgetId = widgetId,
            pageIndex = pageIndex,
            slotIndex = slotIndex,
            boundsInRoot = bounds
        )
    }

    fun registerEmptySlot(
        pageIndex: Int,
        slotIndex: Int,
        bounds: Rect
    ) {
        emptySlots["${pageIndex}_$slotIndex"] = RegisteredEmptySlot(
            pageIndex = pageIndex,
            slotIndex = slotIndex,
            boundsInRoot = bounds
        )
    }

    fun startDrag(
        payload: HomeDragPayload,
        pointerId: PointerId,
        pointerPosInRoot: Offset,
        tileBoundsInRoot: Rect
    ) {
        activeDrag = ActiveHomeDrag(
            payload = payload,
            pointerId = pointerId,
            startPointerInRoot = pointerPosInRoot,
            currentPointerInRoot = pointerPosInRoot,
            initialCenterInRoot = tileBoundsInRoot.center,
            maxTravelPx = 0f
        )
    }

    fun updatePointer(pointerPosInRoot: Offset) {
        val current = activeDrag ?: return
        val dist = (pointerPosInRoot - current.startPointerInRoot).getDistance()
        activeDrag = current.copy(
            currentPointerInRoot = pointerPosInRoot,
            maxTravelPx = maxOf(current.maxTravelPx, dist)
        )
    }

    fun clearDrag(): ActiveHomeDrag? {
        val finished = activeDrag
        activeDrag = null
        return finished
    }

    private fun findNearestGridSlotIndex(
        currentPage: Int,
        dragCenterInRoot: Offset
    ): Int {
        var bestSlot = 0
        var bestDist = Double.MAX_VALUE

        emptySlots.values.forEach { slot ->
            if (slot.pageIndex == currentPage) {
                val d = hypot(
                    (dragCenterInRoot.x - slot.boundsInRoot.center.x).toDouble(),
                    (dragCenterInRoot.y - slot.boundsInRoot.center.y).toDouble()
                )
                if (d < bestDist) {
                    bestDist = d
                    bestSlot = slot.slotIndex
                }
            }
        }
        appSlots.values.forEach { slot ->
            if (slot.pageIndex == currentPage && slot.indexInPage >= 0) {
                val d = hypot(
                    (dragCenterInRoot.x - slot.boundsInRoot.center.x).toDouble(),
                    (dragCenterInRoot.y - slot.boundsInRoot.center.y).toDouble()
                )
                if (d < bestDist) {
                    bestDist = d
                    bestSlot = slot.indexInPage
                }
            }
        }
        nestSlots.values.forEach { slot ->
            if (slot.pageIndex == currentPage && slot.indexInPage >= 0) {
                val d = hypot(
                    (dragCenterInRoot.x - slot.boundsInRoot.center.x).toDouble(),
                    (dragCenterInRoot.y - slot.boundsInRoot.center.y).toDouble()
                )
                if (d < bestDist) {
                    bestDist = d
                    bestSlot = slot.indexInPage
                }
            }
        }

        return bestSlot
    }

    fun resolveDropTarget(
        currentPage: Int,
        dragCenterInRoot: Offset,
        payload: HomeDragPayload,
        mergeRadiusPx: Float
    ): HomeDropTarget {
        return when (payload) {
            is HomeDragPayload.App -> {
                val pageApps = appSlots.values.filter { it.pageIndex == currentPage }
                val pageNests = nestSlots.values.filter { it.pageIndex == currentPage }

                // 1) Check direct overlap with an existing folder
                val hitNest = pageNests.minByOrNull {
                    hypot(
                        (dragCenterInRoot.x - it.boundsInRoot.center.x).toDouble(),
                        (dragCenterInRoot.y - it.boundsInRoot.center.y).toDouble()
                    )
                }?.takeIf { nest ->
                    val dist = (dragCenterInRoot - nest.boundsInRoot.center).getDistance()
                    dist <= mergeRadiusPx || nest.boundsInRoot.contains(dragCenterInRoot)
                }
                if (hitNest != null) {
                    return HomeDropTarget.AddToExistingNest(hitNest.nestId)
                }

                // 2) Check overlap with another app icon -> instant folder merge!
                val otherApps = pageApps.filter { it.componentKey != payload.app.componentKey }
                val hitApp = otherApps.minByOrNull {
                    hypot(
                        (dragCenterInRoot.x - it.boundsInRoot.center.x).toDouble(),
                        (dragCenterInRoot.y - it.boundsInRoot.center.y).toDouble()
                    )
                }?.takeIf { target ->
                    val dist = (dragCenterInRoot - target.boundsInRoot.center).getDistance()
                    dist <= mergeRadiusPx
                }
                if (hitApp != null) {
                    return HomeDropTarget.MergeAppsIntoFolder(hitApp.componentKey)
                }

                // 3) Otherwise snap freely to the nearest grid slot (empty or occupied) on currentPage
                val targetSlot = findNearestGridSlotIndex(currentPage, dragCenterInRoot)
                HomeDropTarget.MoveAppToSlot(currentPage, targetSlot)
            }

            is HomeDragPayload.Nest -> {
                val targetSlot = findNearestGridSlotIndex(currentPage, dragCenterInRoot)
                HomeDropTarget.MoveNestToSlot(currentPage, targetSlot)
            }

            is HomeDragPayload.Widget -> {
                val targetSlot = findNearestGridSlotIndex(currentPage, dragCenterInRoot)
                HomeDropTarget.MoveWidgetToSlot(currentPage, targetSlot)
            }
        }
    }
}

@Composable
fun EmptyHomeGridSlot(
    pageIndex: Int,
    slotIndex: Int,
    dragController: HomeDragController,
    isDropTargetHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    DisposableEffect(dragController, pageIndex, slotIndex) {
        onDispose {
            dragController.emptySlots.remove("${pageIndex}_$slotIndex")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                dragController.registerEmptySlot(pageIndex, slotIndex, coords.boundsInRoot())
            }
            .then(
                when {
                    isDropTargetHighlighted -> Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(18.dp)
                        )
                    dragController.isDragging -> Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    else -> Modifier
                }
            )
    )
}

@Composable
fun rememberHomeDragController(): HomeDragController = remember { HomeDragController() }

/**
 * Root-level floating drag layer placed ABOVE HorizontalPager so dragging across rows,
 * columns, and horizontal layers is 100% locked to the finger in physical screen coordinates.
 */
@Composable
fun HomeDragOverlayHost(
    controller: HomeDragController,
    currentPage: Int,
    pageCount: Int,
    onSwitchToPage: (Int) -> Unit,
    onAddNewPageAndSwitch: () -> Unit,
    onTapHoldAppNoMove: (AppInfo) -> Unit,
    onTapHoldNestNoMove: (OwlNest) -> Unit,
    onTapHoldWidgetNoMove: (PlacedWidget) -> Unit = {},
    onCommitDrop: (HomeDragPayload, HomeDropTarget) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (hoveredDropTarget: HomeDropTarget?) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val dragThresholdPx = with(density) { 14.dp.toPx() }
    val mergeRadiusPx = with(density) { 28.dp.toPx() }
    val edgeZoneWidthPx = with(density) { 42.dp.toPx() }

    val latestCurrentPage by rememberUpdatedState(currentPage)
    val latestPageCount by rememberUpdatedState(pageCount)
    val latestOnSwitchToPage by rememberUpdatedState(onSwitchToPage)
    val latestOnAddNewPage by rememberUpdatedState(onAddNewPageAndSwitch)
    val latestOnTapHoldApp by rememberUpdatedState(onTapHoldAppNoMove)
    val latestOnTapHoldNest by rememberUpdatedState(onTapHoldNestNoMove)
    val latestOnTapHoldWidget by rememberUpdatedState(onTapHoldWidgetNoMove)
    val latestOnCommitDrop by rememberUpdatedState(onCommitDrop)

    val active = controller.activeDrag
    val hoveredTarget = remember(active, currentPage) {
        if (active == null || active.maxTravelPx < dragThresholdPx) {
            null
        } else {
            controller.resolveDropTarget(
                currentPage = currentPage,
                dragCenterInRoot = active.currentCenterInRoot,
                payload = active.payload,
                mergeRadiusPx = mergeRadiusPx
            )
        }
    }

    // Physical edge hover detection (-1 = physical left edge, +1 = physical right edge, 0 = none)
    val edgeHoverPhysicalDir = remember(active, controller.containerBoundsInRoot) {
        if (active == null || active.maxTravelPx < dragThresholdPx || controller.containerBoundsInRoot.width <= 0f) {
            0
        } else {
            val px = active.currentPointerInRoot.x
            when {
                px <= controller.containerBoundsInRoot.left + edgeZoneWidthPx -> -1
                px >= controller.containerBoundsInRoot.right - edgeZoneWidthPx -> 1
                else -> 0
            }
        }
    }

    // Map physical left/right edge to logical page delta respecting RTL vs LTR HorizontalPager
    val logicalEdgePageDelta = remember(edgeHoverPhysicalDir, isRtl) {
        when (edgeHoverPhysicalDir) {
            -1 -> if (isRtl) +1 else -1
            +1 -> if (isRtl) -1 else +1
            else -> 0
        }
    }

    val canFlipToEdge = remember(logicalEdgePageDelta, currentPage, pageCount) {
        when {
            logicalEdgePageDelta < 0 -> currentPage > 0
            logicalEdgePageDelta > 0 -> currentPage < pageCount - 1 || pageCount < 8
            else -> false
        }
    }

    // Auto-switch layer after hovering at the physical left/right screen edge
    LaunchedEffect(logicalEdgePageDelta, currentPage, active != null) {
        if (active != null && logicalEdgePageDelta != 0 && canFlipToEdge) {
            delay(440L)
            if (controller.activeDrag != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val target = latestCurrentPage + logicalEdgePageDelta
                if (target >= latestPageCount && latestPageCount < 8) {
                    latestOnAddNewPage()
                } else if (target in 0 until latestPageCount) {
                    latestOnSwitchToPage(target)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                controller.containerBoundsInRoot = coords.boundsInRoot()
            }
            .pointerInput(controller) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val currentDrag = controller.activeDrag ?: continue

                        val change = event.changes.firstOrNull { it.id == currentDrag.pointerId }
                            ?: event.changes.firstOrNull()
                        if (change != null) {
                            change.consume()
                            val pointerInRoot = controller.containerBoundsInRoot.topLeft + change.position
                            controller.updatePointer(pointerInRoot)

                            if (!change.pressed || event.changes.none { it.pressed }) {
                                val finished = controller.clearDrag()
                                if (finished != null) {
                                    if (finished.maxTravelPx < dragThresholdPx) {
                                        when (val p = finished.payload) {
                                            is HomeDragPayload.App -> latestOnTapHoldApp(p.app)
                                            is HomeDragPayload.Nest -> latestOnTapHoldNest(p.nest)
                                            is HomeDragPayload.Widget -> latestOnTapHoldWidget(p.widget)
                                        }
                                    } else {
                                        val finalTarget = controller.resolveDropTarget(
                                            currentPage = latestCurrentPage,
                                            dragCenterInRoot = finished.currentCenterInRoot,
                                            payload = finished.payload,
                                            mergeRadiusPx = mergeRadiusPx
                                        )
                                        latestOnCommitDrop(finished.payload, finalTarget)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content(hoveredTarget)

        // Force physical LTR coordinate space for edge glows & floating ghost so
        // Alignment.TopStart is ALWAYS physical (0,0) and left/right edge glows never mirror in RTL.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                val leftAlpha by animateFloatAsState(
                    targetValue = if (edgeHoverPhysicalDir == -1 && canFlipToEdge) 1f else 0f,
                    label = "left_edge_glow"
                )
                val rightAlpha by animateFloatAsState(
                    targetValue = if (edgeHoverPhysicalDir == 1 && canFlipToEdge) 1f else 0f,
                    label = "right_edge_glow"
                )

                if (leftAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(26.dp)
                            .fillMaxHeight(0.58f)
                            .graphicsLayer { alpha = leftAlpha }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                            )
                    )
                }
                if (rightAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(26.dp)
                            .fillMaxHeight(0.58f)
                            .graphicsLayer { alpha = rightAlpha }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    )
                                ),
                                RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                            )
                    )
                }

                // Floating Ghost Icon / Widget strictly locked to finger in physical screen coordinates
                if (active != null) {
                    val containerTopLeft = controller.containerBoundsInRoot.topLeft
                    val localCenter = active.currentCenterInRoot - containerTopLeft
                    val isMergePreview = hoveredTarget is HomeDropTarget.MergeAppsIntoFolder ||
                        hoveredTarget is HomeDropTarget.AddToExistingNest

                    val ghostWidthDp = when (val p = active.payload) {
                        is HomeDragPayload.App -> p.iconWidth + 16.dp
                        is HomeDragPayload.Nest -> (p.iconSize * p.nest.widthScale + 16.dp).coerceIn(48.dp, 260.dp)
                        is HomeDragPayload.Widget -> p.previewWidthDp.coerceIn(56.dp, 260.dp)
                    }
                    val ghostHeightDp = when (val p = active.payload) {
                        is HomeDragPayload.App -> p.iconHeight + 16.dp
                        is HomeDragPayload.Nest -> (p.iconSize * p.nest.heightScale + 16.dp).coerceIn(48.dp, 260.dp)
                        is HomeDragPayload.Widget -> p.previewHeightDp.coerceIn(56.dp, 220.dp)
                    }
                    val ghostWidthPx = with(density) { ghostWidthDp.toPx() }
                    val ghostHeightPx = with(density) { ghostHeightDp.toPx() }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .zIndex(50f)
                            .size(width = ghostWidthDp, height = ghostHeightDp)
                            .graphicsLayer {
                                translationX = localCenter.x - ghostWidthPx / 2f
                                translationY = localCenter.y - ghostHeightPx / 2f
                                scaleX = if (isMergePreview) 0.94f else 1.08f
                                scaleY = if (isMergePreview) 0.94f else 1.08f
                                alpha = 0.92f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMergePreview) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            )
                        }

                        when (val p = active.payload) {
                            is HomeDragPayload.App -> {
                                UnalteredAppIconArtwork(
                                    app = p.app,
                                    iconShape = p.iconShape,
                                    iconSize = p.iconSize,
                                    iconWidth = p.iconWidth,
                                    iconHeight = p.iconHeight,
                                    showFrameBackdrop = p.showFrameBackdrop
                                )
                            }

                            is HomeDragPayload.Nest -> {
                                NestVisualPreview(
                                    nest = p.nest,
                                    appsInNest = p.appsInNest,
                                    effectiveShape = p.nest.shapeOverride ?: p.iconShape,
                                    iconSize = p.iconSize,
                                    glassOpacity = p.glassOpacity
                                )
                            }

                            is HomeDragPayload.Widget -> {
                                val shape = when (val ov = p.widget.shapeOverride) {
                                    null, IconShapeStyle.ORIGINAL -> RoundedCornerShape(p.widget.cornerRadiusDp.dp)
                                    else -> ov.toComposeShape()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                        .border(2.dp, MaterialTheme.colorScheme.primary, shape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.widget.nativeType?.name ?: "WIDGET",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NestVisualPreview(
    nest: OwlNest,
    appsInNest: List<AppInfo>,
    effectiveShape: IconShapeStyle,
    iconSize: Dp,
    glassOpacity: Float
) {
    val shape = if (effectiveShape == IconShapeStyle.ORIGINAL) {
        RoundedCornerShape(20.dp)
    } else {
        effectiveShape.toComposeShape()
    }
    val wDp = (iconSize * nest.widthScale + 4.dp).coerceIn(38.dp, 240.dp)
    val hDp = (iconSize * nest.heightScale + 4.dp).coerceIn(38.dp, 240.dp)
    Box(
        modifier = Modifier
            .size(width = wDp, height = hDp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = glassOpacity))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
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
}
