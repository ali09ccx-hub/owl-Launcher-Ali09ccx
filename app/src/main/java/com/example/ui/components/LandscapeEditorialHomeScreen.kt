package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.FolderSizeMode
import com.example.model.GestureId
import com.example.model.OwlNest
import com.example.model.PlacedWidgetKind
import com.example.model.WidgetSize
import com.example.ui.ItemCustomizationTarget
import com.example.ui.LauncherUiState
import com.example.ui.theme.AmiriEditorialFamily
import com.example.widgets.OwlAppWidgetHost

@Composable
fun LandscapeEditorialHomeScreen(
    uiState: LauncherUiState,
    pagerState: PagerState,
    widgetHost: OwlAppWidgetHost,
    onGesture: (GestureId) -> Unit,
    onOpenRecents: () -> Unit,
    onLongPressHome: () -> Unit,
    onCloseControlBar: () -> Unit,
    onToggleEditMode: () -> Unit,
    onOpenSettings: (Int) -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onOpenCreateNest: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onUnpinHomeApp: (AppInfo) -> Unit,
    onReorderOrMoveApp: (String, Int, Int?) -> Unit,
    onMergeAppsIntoFolder: (String, String, Int) -> Unit,
    onDropAppIntoNest: (String, String) -> Unit,
    onReorderOrMoveNest: (String, Int, Int?) -> Unit,
    onReorderOrMoveWidget: (String, Int, Int?) -> Unit,
    onDeleteNest: (String) -> Unit,
    onCustomizeItem: (ItemCustomizationTarget) -> Unit,
    onOpenNest: (OwlNest) -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onCycleWisdom: () -> Unit,
    onPickGifForWidget: (String, android.net.Uri) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onMoveStickyText: (Int, Float, Float) -> Unit,
    onEditStickyText: (Int) -> Unit,
    onClearStickyText: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.prefs
    val systemLayoutDirection = LocalLayoutDirection.current
    val dragController = rememberHomeDragController()
    val pageListStates = remember { mutableStateMapOf<Int, LazyListState>() }

    HomeDragOverlayHost(
        controller = dragController,
        currentPage = pagerState.currentPage,
        pageCount = prefs.pageCount,
        onSwitchToPage = onJumpToPage,
        onAddNewPageAndSwitch = onAddPage,
        onTapHoldAppNoMove = onShowAppMenu,
        onTapHoldNestNoMove = { nest ->
            onCustomizeItem(ItemCustomizationTarget.NestTarget(nest))
        },
        onTapHoldWidgetNoMove = { widget ->
            onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
        },
        onCommitDrop = { payload, target ->
            when (payload) {
                is HomeDragPayload.App -> when (target) {
                    is HomeDropTarget.MergeAppsIntoFolder -> {
                        onMergeAppsIntoFolder(
                            payload.app.componentKey,
                            target.targetAppKey,
                            pagerState.currentPage
                        )
                    }
                    is HomeDropTarget.AddToExistingNest -> {
                        onDropAppIntoNest(payload.app.componentKey, target.targetNestId)
                    }
                    is HomeDropTarget.MoveAppToSlot -> {
                        onReorderOrMoveApp(
                            payload.app.componentKey,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    is HomeDropTarget.MoveNestToSlot -> {
                        onReorderOrMoveApp(
                            payload.app.componentKey,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    is HomeDropTarget.MoveWidgetToSlot -> {
                        onReorderOrMoveApp(
                            payload.app.componentKey,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                }
                is HomeDragPayload.Nest -> when (target) {
                    is HomeDropTarget.MoveNestToSlot -> {
                        onReorderOrMoveNest(
                            payload.nest.id,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    is HomeDropTarget.MoveAppToSlot -> {
                        onReorderOrMoveNest(
                            payload.nest.id,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    is HomeDropTarget.MoveWidgetToSlot -> {
                        onReorderOrMoveNest(
                            payload.nest.id,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    else -> {
                        onReorderOrMoveNest(payload.nest.id, pagerState.currentPage, null)
                    }
                }
                is HomeDragPayload.Widget -> when (target) {
                    is HomeDropTarget.MoveWidgetToSlot -> {
                        onReorderOrMoveWidget(
                            payload.widget.id,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    is HomeDropTarget.MoveAppToSlot -> {
                        onReorderOrMoveWidget(
                            payload.widget.id,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    is HomeDropTarget.MoveNestToSlot -> {
                        onReorderOrMoveWidget(
                            payload.widget.id,
                            target.targetPage,
                            target.targetSlotIndex
                        )
                    }
                    else -> {
                        onReorderOrMoveWidget(payload.widget.id, pagerState.currentPage, null)
                    }
                }
            }
        },
        modifier = modifier
    ) { hoveredDropTarget ->
        OwlGestureOverlay(
            onGesture = onGesture,
            onLongPressEmptyArea = onLongPressHome,
            isChildDragging = dragController.isDragging,
            canScrollBackward = { pageListStates[pagerState.currentPage]?.canScrollBackward == true },
            canScrollForward = { pageListStates[pagerState.currentPage]?.canScrollForward == true },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                if (!prefs.cleanCanvasMode) {
                    // Force physical LTR on the outer split Row so the vertical dock ALWAYS sits
                    // on the physical RIGHT edge of the screen even when device language is Arabic (RTL).
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Restore user's LayoutDirection inside the main wide magazine icon/folder spread
                            CompositionLocalProvider(LocalLayoutDirection provides systemLayoutDirection) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        beyondViewportPageCount = 1,
                                        userScrollEnabled = !dragController.isDragging,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .testTag("landscape_editorial_spread")
                                    ) { pageIndex ->
                                        val pageOffset = (pagerState.currentPage - pageIndex) +
                                            pagerState.currentPageOffsetFraction
                                        val listState = pageListStates.getOrPut(pageIndex) { LazyListState() }

                                        LandscapeWideSpreadPage(
                                            pageIndex = pageIndex,
                                            listState = listState,
                                            uiState = uiState,
                                            widgetHost = widgetHost,
                                            dragController = dragController,
                                            hoveredDropTarget = hoveredDropTarget,
                                            onLaunchApp = onLaunchApp,
                                            onShowAppMenu = onShowAppMenu,
                                            onUnpinHomeApp = onUnpinHomeApp,
                                            onDeleteNest = onDeleteNest,
                                            onCustomizeItem = onCustomizeItem,
                                            onOpenNest = onOpenNest,
                                            onToggleFocusTimer = onToggleFocusTimer,
                                            onResetFocusTimer = onResetFocusTimer,
                                            onToggleFlashlight = onToggleFlashlight,
                                            onOpenNoteEditor = onOpenNoteEditor,
                                            onCycleWisdom = onCycleWisdom,
                                            onPickGifForWidget = onPickGifForWidget,
                                            onRemoveWidget = onRemoveWidget,
                                            onMoveStickyText = onMoveStickyText,
                                            onEditStickyText = onEditStickyText,
                                            onClearStickyText = onClearStickyText,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .layerTransitionModifier(prefs.layerTransition, pageOffset)
                                        )
                                    }

                                    if (prefs.pageCount > 1) {
                                        LayerDotsIndicator(
                                            pageCount = prefs.pageCount,
                                            currentPage = pagerState.currentPage,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Physical RIGHT vertical floating dock (pinned apps + optional icon-only Recents button)
                            if (prefs.showDock && (uiState.dockApps.isNotEmpty() || prefs.showRecentsInDock)) {
                                LandscapeRightVerticalDock(
                                    dockApps = uiState.dockApps,
                                    uiState = uiState,
                                    showRecentsButton = prefs.showRecentsInDock,
                                    onOpenRecents = onOpenRecents,
                                    onLaunchApp = onLaunchApp,
                                    onShowAppMenu = onShowAppMenu
                                )
                            }
                        }
                    }
                } else {
                    HomeStickyTextLayer(
                        stickyText = prefs.stickyTexts.firstOrNull { it.pageIndex == pagerState.currentPage },
                        isEditMode = uiState.isEditMode,
                        onMovePosition = { x, y -> onMoveStickyText(pagerState.currentPage, x, y) },
                        onTapToEdit = { onEditStickyText(pagerState.currentPage) },
                        onClearText = { onClearStickyText(pagerState.currentPage) }
                    )
                }

                // Long-Press Revealed Control Bar overlay
                AnimatedVisibility(
                    visible = uiState.isHomeControlBarOpen || uiState.isEditMode,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    HomeLongPressControlBar(
                        currentPage = pagerState.currentPage,
                        pageCount = prefs.pageCount,
                        isEditMode = uiState.isEditMode,
                        onToggleEdit = onToggleEditMode,
                        onOpenSettings = { onOpenSettings(0) },
                        onOpenWidgets = onOpenWidgetPicker,
                        onCreateFolder = onOpenCreateNest,
                        onOpenRecents = onOpenRecents,
                        onOpenGestures = { onOpenSettings(3) },
                        onAddPage = onAddPage,
                        onRemovePage = onRemovePage,
                        onDone = onCloseControlBar,
                        modifier = Modifier.widthIn(max = 720.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeWideSpreadPage(
    pageIndex: Int,
    listState: LazyListState,
    uiState: LauncherUiState,
    widgetHost: OwlAppWidgetHost,
    dragController: HomeDragController,
    hoveredDropTarget: HomeDropTarget?,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onUnpinHomeApp: (AppInfo) -> Unit,
    onDeleteNest: (String) -> Unit,
    onCustomizeItem: (ItemCustomizationTarget) -> Unit,
    onOpenNest: (OwlNest) -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onCycleWisdom: () -> Unit,
    onPickGifForWidget: (String, android.net.Uri) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onMoveStickyText: (Int, Float, Float) -> Unit,
    onEditStickyText: (Int) -> Unit,
    onClearStickyText: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.prefs
    val pageWidgets = uiState.widgetsForPage(pageIndex)
    val pageNests = uiState.nestsForPage(pageIndex)
    val pageApps = uiState.placedAppsForPage(pageIndex)
    val stickyText = prefs.stickyTexts.firstOrNull { it.pageIndex == pageIndex }
    val columns = prefs.landscapeColumns.coerceIn(3, 7)
    val minRows = 4

    val smallNests = remember(pageNests) {
        pageNests.filter { it.folderSize == FolderSizeMode.SIZE_1X1 }
    }
    val bigNests = remember(pageNests) {
        pageNests.filter { it.folderSize != FolderSizeMode.SIZE_1X1 }
    }

    val allocation = remember(smallNests, pageApps, columns) {
        allocateHomeGridSlots(
            smallNests = smallNests,
            pageApps = pageApps,
            columns = columns,
            minRows = minRows,
            isLandscape = true
        )
    }

    val activeDragPayload = dragController.activeDrag?.payload
    val draggedAppKey = (activeDragPayload as? HomeDragPayload.App)?.app?.componentKey
    val draggedNestId = (activeDragPayload as? HomeDragPayload.Nest)?.nest?.id
    val draggedWidgetId = (activeDragPayload as? HomeDragPayload.Widget)?.widget?.id

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        if (bigNests.isEmpty() && allocation.totalRows <= minRows) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val hGap = 12.dp
                val vGap = 8.dp
                val cellWidth = ((maxWidth - hGap * (columns - 1)) / columns).coerceAtLeast(42.dp)
                val cellHeight = ((maxHeight - vGap * (allocation.totalRows - 1)) / allocation.totalRows).coerceAtLeast(48.dp)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(vGap)
                ) {
                    for (row in 0 until allocation.totalRows) {
                        LandscapeGridSlotRow(
                            row = row,
                            columns = columns,
                            pageIndex = pageIndex,
                            allocation = allocation,
                            uiState = uiState,
                            dragController = dragController,
                            hoveredDropTarget = hoveredDropTarget,
                            draggedAppKey = draggedAppKey,
                            draggedNestId = draggedNestId,
                            onLaunchApp = onLaunchApp,
                            onShowAppMenu = onShowAppMenu,
                            onUnpinHomeApp = onUnpinHomeApp,
                            onDeleteNest = onDeleteNest,
                            onCustomizeItem = onCustomizeItem,
                            onOpenNest = onOpenNest,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }

                // Freely Positioned Widgets Layer across Wide Landscape Spread
                pageWidgets.forEachIndexed { idx, widget ->
                    val rawSlot = widget.landscapeSlotIndex.takeIf { it >= 0 } ?: widget.slotIndex
                    val totalSlots = columns * allocation.totalRows
                    val effectiveSlot = if (rawSlot >= 0) {
                        rawSlot.coerceIn(0, (totalSlots - 1).coerceAtLeast(0))
                    } else {
                        ((idx * 2) % totalSlots.coerceAtLeast(1))
                    }
                    val row = (effectiveSlot / columns).coerceIn(0, (allocation.totalRows - 1).coerceAtLeast(0))
                    val col = (effectiveSlot % columns).coerceIn(0, columns - 1)

                    val wDp = (widget.customWidthDp?.dp ?: when (widget.size) {
                        WidgetSize.COMPACT -> cellWidth
                        WidgetSize.WIDE -> minOf(maxWidth, cellWidth * 2 + hGap)
                        WidgetSize.TALL -> minOf(maxWidth, cellWidth * 2 + hGap)
                    }).coerceIn(48.dp, maxWidth)

                    val hDp = (widget.customHeightDp?.dp ?: when (widget.size) {
                        WidgetSize.COMPACT -> cellHeight
                        WidgetSize.WIDE -> minOf(maxHeight, 112.dp)
                        WidgetSize.TALL -> minOf(maxHeight, 180.dp)
                    }).coerceIn(48.dp, maxHeight)

                    val baseCellX = (cellWidth + hGap) * col
                    val xDp = if (wDp <= cellWidth + 10.dp) {
                        (baseCellX + (cellWidth - wDp) / 2f).coerceIn(0.dp, (maxWidth - wDp).coerceAtLeast(0.dp))
                    } else {
                        baseCellX.coerceIn(0.dp, (maxWidth - wDp).coerceAtLeast(0.dp))
                    }
                    val baseCellY = (cellHeight + vGap) * row
                    val yDp = if (hDp <= cellHeight + 10.dp) {
                        (baseCellY + (cellHeight - hDp) / 2f).coerceIn(0.dp, (maxHeight - hDp).coerceAtLeast(0.dp))
                    } else {
                        baseCellY.coerceIn(0.dp, (maxHeight - hDp).coerceAtLeast(0.dp))
                    }

                    val dragPayload = HomeDragPayload.Widget(
                        widget = widget,
                        sourcePage = pageIndex,
                        previewWidthDp = wDp,
                        previewHeightDp = hDp
                    )
                    val posModifier = Modifier.offset(x = xDp, y = yDp)

                    when (widget.kind) {
                        PlacedWidgetKind.OWL_NATIVE -> {
                            OwlNativeWidgetCard(
                                widget = widget,
                                glassOpacity = prefs.glassOpacity,
                                deviceStatus = uiState.deviceStatus,
                                focusSecondsRemaining = uiState.focusSecondsRemaining,
                                focusTimerRunning = uiState.focusTimerRunning,
                                dispatchNoteText = prefs.dispatchNoteText,
                                wisdomIndex = uiState.wisdomIndex,
                                isEditMode = uiState.isEditMode,
                                onToggleFocusTimer = onToggleFocusTimer,
                                onResetFocusTimer = onResetFocusTimer,
                                onToggleFlashlight = onToggleFlashlight,
                                onOpenNoteEditor = onOpenNoteEditor,
                                onCycleWisdom = onCycleWisdom,
                                onPickGifForWidget = { uri -> onPickGifForWidget(widget.id, uri) },
                                onCustomize = {
                                    onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
                                },
                                onRemove = { onRemoveWidget(widget.id) },
                                dragController = dragController,
                                dragPayload = dragPayload,
                                pageIndex = pageIndex,
                                slotIndex = effectiveSlot,
                                forcedWidthDp = wDp,
                                forcedHeightDp = hDp,
                                isHiddenWhileDragged = draggedWidgetId == widget.id,
                                modifier = posModifier
                            )
                        }

                        PlacedWidgetKind.ANDROID_APPWIDGET -> {
                            SystemWidgetCard(
                                widget = widget,
                                widgetHost = widgetHost,
                                glassOpacity = prefs.glassOpacity,
                                isEditMode = uiState.isEditMode,
                                onCustomize = {
                                    onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
                                },
                                onRemove = { onRemoveWidget(widget.id) },
                                dragController = dragController,
                                dragPayload = dragPayload,
                                pageIndex = pageIndex,
                                slotIndex = effectiveSlot,
                                forcedWidthDp = wDp,
                                forcedHeightDp = hDp,
                                isHiddenWhileDragged = draggedWidgetId == widget.id,
                                modifier = posModifier
                            )
                        }
                    }
                }

                HomeStickyTextLayer(
                    stickyText = stickyText,
                    isEditMode = uiState.isEditMode,
                    onMovePosition = { x, y -> onMoveStickyText(pageIndex, x, y) },
                    onTapToEdit = { onEditStickyText(pageIndex) },
                    onClearText = { onClearStickyText(pageIndex) }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pageWidgets.isNotEmpty()) {
                        val widgetRows = pageWidgets.chunked(2)
                        itemsIndexed(
                            items = widgetRows,
                            key = { idx, row -> "land_wrow_${pageIndex}_${row.first().id}_$idx" }
                        ) { _, rowWidgets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowWidgets.forEach { widget ->
                                    val wDp = widget.customWidthDp?.dp
                                    val hDp = widget.customHeightDp?.dp
                                    val dragPayload = HomeDragPayload.Widget(
                                        widget = widget,
                                        sourcePage = pageIndex,
                                        previewWidthDp = wDp ?: 200.dp,
                                        previewHeightDp = hDp ?: 110.dp
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (widget.kind) {
                                            PlacedWidgetKind.OWL_NATIVE -> {
                                                OwlNativeWidgetCard(
                                                    widget = widget,
                                                    glassOpacity = prefs.glassOpacity,
                                                    deviceStatus = uiState.deviceStatus,
                                                    focusSecondsRemaining = uiState.focusSecondsRemaining,
                                                    focusTimerRunning = uiState.focusTimerRunning,
                                                    dispatchNoteText = prefs.dispatchNoteText,
                                                    wisdomIndex = uiState.wisdomIndex,
                                                    isEditMode = uiState.isEditMode,
                                                    onToggleFocusTimer = onToggleFocusTimer,
                                                    onResetFocusTimer = onResetFocusTimer,
                                                    onToggleFlashlight = onToggleFlashlight,
                                                    onOpenNoteEditor = onOpenNoteEditor,
                                                    onCycleWisdom = onCycleWisdom,
                                                    onPickGifForWidget = { uri -> onPickGifForWidget(widget.id, uri) },
                                                    onCustomize = {
                                                        onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
                                                    },
                                                    onRemove = { onRemoveWidget(widget.id) },
                                                    dragController = dragController,
                                                    dragPayload = dragPayload,
                                                    pageIndex = pageIndex,
                                                    slotIndex = widget.landscapeSlotIndex.takeIf { it >= 0 } ?: widget.slotIndex,
                                                    forcedWidthDp = wDp,
                                                    forcedHeightDp = hDp,
                                                    isHiddenWhileDragged = draggedWidgetId == widget.id
                                                )
                                            }

                                            PlacedWidgetKind.ANDROID_APPWIDGET -> {
                                                SystemWidgetCard(
                                                    widget = widget,
                                                    widgetHost = widgetHost,
                                                    glassOpacity = prefs.glassOpacity,
                                                    isEditMode = uiState.isEditMode,
                                                    onCustomize = {
                                                        onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
                                                    },
                                                    onRemove = { onRemoveWidget(widget.id) },
                                                    dragController = dragController,
                                                    dragPayload = dragPayload,
                                                    pageIndex = pageIndex,
                                                    slotIndex = widget.landscapeSlotIndex.takeIf { it >= 0 } ?: widget.slotIndex,
                                                    forcedWidthDp = wDp,
                                                    forcedHeightDp = hDp,
                                                    isHiddenWhileDragged = draggedWidgetId == widget.id
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowWidgets.size == 1 && pageWidgets.size > 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Big Folders arranged cleanly across wide landscape width
                    if (bigNests.isNotEmpty()) {
                        val bigRows = bigNests.chunked(2)
                        itemsIndexed(
                            items = bigRows,
                            key = { idx, row -> "land_bignest_row_${pageIndex}_${row.first().id}_$idx" }
                        ) { _, rowNests ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowNests.forEach { nest ->
                                    val isMergeTarget =
                                        (hoveredDropTarget as? HomeDropTarget.AddToExistingNest)?.targetNestId == nest.id
                                    Box(modifier = Modifier.weight(1f)) {
                                        OwlNestTile(
                                            nest = nest,
                                            appsInNest = nest.appKeys.mapNotNull { uiState.findApp(it) },
                                            globalIconShape = prefs.iconShape,
                                            iconSize = prefs.iconSizeDp.dp,
                                            showLabel = prefs.showAppLabels,
                                            glassOpacity = prefs.glassOpacity,
                                            isEditMode = uiState.isEditMode,
                                            onClick = { onOpenNest(nest) },
                                            onLongClick = {
                                                onCustomizeItem(ItemCustomizationTarget.NestTarget(nest))
                                            },
                                            onLaunchAppDirectly = onLaunchApp,
                                            onDeleteInEditMode = { onDeleteNest(nest.id) },
                                            dragController = dragController,
                                            pageIndex = pageIndex,
                                            indexInPage = -1,
                                            isMergeHighlighted = isMergeTarget
                                        )
                                    }
                                }
                                if (rowNests.size == 1 && bigNests.size > 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Balanced Free-Slot Magazine Spread
                    items(
                        count = allocation.totalRows,
                        key = { row -> "land_spread_slotrow_${pageIndex}_$row" }
                    ) { row ->
                        LandscapeGridSlotRow(
                            row = row,
                            columns = columns,
                            pageIndex = pageIndex,
                            allocation = allocation,
                            uiState = uiState,
                            dragController = dragController,
                            hoveredDropTarget = hoveredDropTarget,
                            draggedAppKey = draggedAppKey,
                            draggedNestId = draggedNestId,
                            onLaunchApp = onLaunchApp,
                            onShowAppMenu = onShowAppMenu,
                            onUnpinHomeApp = onUnpinHomeApp,
                            onDeleteNest = onDeleteNest,
                            onCustomizeItem = onCustomizeItem,
                            onOpenNest = onOpenNest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp)
                        )
                    }
                }

                HomeStickyTextLayer(
                    stickyText = stickyText,
                    isEditMode = uiState.isEditMode,
                    onMovePosition = { x, y -> onMoveStickyText(pageIndex, x, y) },
                    onTapToEdit = { onEditStickyText(pageIndex) },
                    onClearText = { onClearStickyText(pageIndex) }
                )
            }
        }
    }
}

@Composable
private fun LandscapeGridSlotRow(
    row: Int,
    columns: Int,
    pageIndex: Int,
    allocation: HomeGridAllocation,
    uiState: LauncherUiState,
    dragController: HomeDragController,
    hoveredDropTarget: HomeDropTarget?,
    draggedAppKey: String?,
    draggedNestId: String?,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onUnpinHomeApp: (AppInfo) -> Unit,
    onDeleteNest: (String) -> Unit,
    onCustomizeItem: (ItemCustomizationTarget) -> Unit,
    onOpenNest: (OwlNest) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.prefs
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (col in 0 until columns) {
            val slotIndex = row * columns + col
            val entry = allocation.slotMap[slotIndex]
            val isSlotDropHovered = when (hoveredDropTarget) {
                is HomeDropTarget.MoveAppToSlot ->
                    hoveredDropTarget.targetPage == pageIndex && hoveredDropTarget.targetSlotIndex == slotIndex
                is HomeDropTarget.MoveNestToSlot ->
                    hoveredDropTarget.targetPage == pageIndex && hoveredDropTarget.targetSlotIndex == slotIndex
                is HomeDropTarget.MoveWidgetToSlot ->
                    hoveredDropTarget.targetPage == pageIndex && hoveredDropTarget.targetSlotIndex == slotIndex
                else -> false
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (entry) {
                    is HomeGridEntry.NestEntry -> {
                        val nest = entry.nest
                        val appsInNest = nest.appKeys.mapNotNull { uiState.findApp(it) }
                        val isMergeTarget =
                            (hoveredDropTarget as? HomeDropTarget.AddToExistingNest)?.targetNestId == nest.id
                        val iconSize = (prefs.iconSizeDp - 2).coerceAtLeast(42).dp
                        OwlNestTile(
                            nest = nest,
                            appsInNest = appsInNest,
                            globalIconShape = prefs.iconShape,
                            iconSize = iconSize,
                            showLabel = prefs.showAppLabels,
                            glassOpacity = prefs.glassOpacity,
                            isEditMode = uiState.isEditMode,
                            onClick = { onOpenNest(nest) },
                            onLongClick = {
                                onCustomizeItem(ItemCustomizationTarget.NestTarget(nest))
                            },
                            onLaunchAppDirectly = onLaunchApp,
                            onDeleteInEditMode = { onDeleteNest(nest.id) },
                            dragController = dragController,
                            dragPayload = HomeDragPayload.Nest(
                                nest = nest,
                                appsInNest = appsInNest,
                                sourcePage = pageIndex,
                                iconShape = prefs.iconShape,
                                iconSize = iconSize,
                                glassOpacity = prefs.glassOpacity
                            ),
                            pageIndex = pageIndex,
                            indexInPage = slotIndex,
                            isMergeHighlighted = isMergeTarget,
                            isHiddenWhileDragged = draggedNestId == nest.id
                        )
                    }

                    is HomeGridEntry.AppEntry -> {
                        val placed = entry.placed
                        val app = entry.app
                        val baseSize = (prefs.iconSizeDp - 2).coerceAtLeast(40)
                        val itemW = (baseSize * placed.effectiveWidthScale).coerceIn(32f, 200f).dp
                        val itemH = (baseSize * placed.effectiveHeightScale).coerceIn(32f, 200f).dp
                        val itemSize = maxOf(itemW, itemH)
                        val effectiveShape = placed.shapeOverride ?: prefs.iconShape
                        val isMergeTarget =
                            (hoveredDropTarget as? HomeDropTarget.MergeAppsIntoFolder)?.targetAppKey == app.componentKey
                        AppIconTile(
                            app = app,
                            iconShape = effectiveShape,
                            iconSize = itemSize,
                            iconWidth = itemW,
                            iconHeight = itemH,
                            showFrameBackdrop = prefs.showIconFrameBackdrop,
                            showLabel = prefs.showAppLabels,
                            isEditMode = uiState.isEditMode,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onShowAppMenu(app) },
                            onCustomizeInEditMode = {
                                onCustomizeItem(
                                    ItemCustomizationTarget.AppTarget(placed, app)
                                )
                            },
                            onRemoveInEditMode = { onUnpinHomeApp(app) },
                            dragController = dragController,
                            dragPayload = HomeDragPayload.App(
                                app = app,
                                placed = placed,
                                sourcePage = pageIndex,
                                iconShape = effectiveShape,
                                iconSize = itemSize,
                                iconWidth = itemW,
                                iconHeight = itemH,
                                showFrameBackdrop = prefs.showIconFrameBackdrop
                            ),
                            pageIndex = pageIndex,
                            indexInPage = slotIndex,
                            isMergeHighlighted = isMergeTarget,
                            isHiddenWhileDragged = draggedAppKey == app.componentKey
                        )
                    }

                    null -> {
                        EmptyHomeGridSlot(
                            pageIndex = pageIndex,
                            slotIndex = slotIndex,
                            dragController = dragController,
                            isDropTargetHighlighted = isSlotDropHovered
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeRightVerticalDock(
    dockApps: List<AppInfo>,
    uiState: LauncherUiState,
    showRecentsButton: Boolean,
    onOpenRecents: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit
) {
    val prefs = uiState.prefs
    val dockIconSize = (prefs.iconSizeDp - 10).coerceIn(36, 48).dp
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = (prefs.glassOpacity + 0.14f).coerceAtMost(0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier
            .width(68.dp)
            .testTag("landscape_right_vertical_dock")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dockApps.take(5).forEach { app ->
                AppIconTile(
                    app = app,
                    iconShape = prefs.iconShape,
                    iconSize = dockIconSize,
                    showFrameBackdrop = prefs.showIconFrameBackdrop,
                    showLabel = false,
                    isEditMode = false,
                    onClick = { onLaunchApp(app) },
                    onLongClick = { onShowAppMenu(app) }
                )
            }

            if (showRecentsButton) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                            CircleShape
                        )
                        .clickable(onClick = onOpenRecents)
                        .testTag("dock_recents_btn_landscape"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CropSquare,
                        contentDescription = stringResource(R.string.recents_desc),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditorialSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontFamily = AmiriEditorialFamily,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.22f)
        )
    }
}
