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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    onCustomizeItem: (ItemCustomizationTarget) -> Unit,
    onOpenNest: (OwlNest) -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onCycleWisdom: () -> Unit,
    onPickGifForWidget: (String, android.net.Uri) -> Unit,
    onCycleWidgetSize: (String) -> Unit,
    onMoveWidget: (String, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
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
                    else -> {
                        onReorderOrMoveNest(payload.nest.id, pagerState.currentPage, null)
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
                                            onCustomizeItem = onCustomizeItem,
                                            onOpenNest = onOpenNest,
                                            onToggleFocusTimer = onToggleFocusTimer,
                                            onResetFocusTimer = onResetFocusTimer,
                                            onToggleFlashlight = onToggleFlashlight,
                                            onOpenNoteEditor = onOpenNoteEditor,
                                            onCycleWisdom = onCycleWisdom,
                                            onPickGifForWidget = onPickGifForWidget,
                                            onCycleWidgetSize = onCycleWidgetSize,
                                            onMoveWidget = onMoveWidget,
                                            onRemoveWidget = onRemoveWidget,
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
    onCustomizeItem: (ItemCustomizationTarget) -> Unit,
    onOpenNest: (OwlNest) -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onCycleWisdom: () -> Unit,
    onPickGifForWidget: (String, android.net.Uri) -> Unit,
    onCycleWidgetSize: (String) -> Unit,
    onMoveWidget: (String, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.prefs
    val pageWidgets = uiState.widgetsForPage(pageIndex)
    val pageNests = uiState.nestsForPage(pageIndex)
    val pageApps = uiState.placedAppsForPage(pageIndex)
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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        if (pageWidgets.isEmpty() && bigNests.isEmpty() && allocation.totalRows <= minRows) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        onCustomizeItem = onCustomizeItem,
                        onOpenNest = onOpenNest,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 920.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Optional user-added widgets paired cleanly in 2-column magazine rows when multiple
                if (pageWidgets.isNotEmpty()) {
                    val widgetRows = pageWidgets.chunked(2)
                    itemsIndexed(
                        items = widgetRows,
                        key = { idx, row -> "land_wrow_${pageIndex}_${row.first().id}_$idx" }
                    ) { rowIdx, rowWidgets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowWidgets.forEachIndexed { colIdx, widget ->
                                val flatIdx = rowIdx * 2 + colIdx
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
                                                onCycleSize = { onCycleWidgetSize(widget.id) },
                                                onMoveUp = { if (flatIdx > 0) onMoveWidget(widget.id, -1) },
                                                onMoveDown = { onMoveWidget(widget.id, 1) },
                                                onRemove = { onRemoveWidget(widget.id) }
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
                                                onCycleSize = { onCycleWidgetSize(widget.id) },
                                                onMoveUp = { if (flatIdx > 0) onMoveWidget(widget.id, -1) },
                                                onMoveDown = { onMoveWidget(widget.id, 1) },
                                                onRemove = { onRemoveWidget(widget.id) }
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
                        onCustomizeItem = onCustomizeItem,
                        onOpenNest = onOpenNest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                    )
                }
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
                        val itemSize = ((prefs.iconSizeDp - 2) * placed.scale)
                            .coerceIn(38f, 80f).dp
                        val effectiveShape = placed.shapeOverride ?: prefs.iconShape
                        val isMergeTarget =
                            (hoveredDropTarget as? HomeDropTarget.MergeAppsIntoFolder)?.targetAppKey == app.componentKey
                        AppIconTile(
                            app = app,
                            iconShape = effectiveShape,
                            iconSize = itemSize,
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
