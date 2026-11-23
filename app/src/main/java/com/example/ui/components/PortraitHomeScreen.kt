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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.FolderSizeMode
import com.example.model.GestureId
import com.example.model.LayerTransitionStyle
import com.example.model.OwlNest
import com.example.model.PlacedWidgetKind
import com.example.ui.ItemCustomizationTarget
import com.example.ui.LauncherUiState
import com.example.widgets.OwlAppWidgetHost
import kotlin.math.abs

@Composable
fun PortraitHomeScreen(
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AnimatedVisibility(
                    visible = !prefs.cleanCanvasMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            userScrollEnabled = !dragController.isDragging,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("home_layers_pager")
                        ) { pageIndex ->
                            val pageOffset = (pagerState.currentPage - pageIndex) +
                                pagerState.currentPageOffsetFraction
                            val listState = pageListStates.getOrPut(pageIndex) { LazyListState() }

                            HomeLayerPageContent(
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
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                if (prefs.cleanCanvasMode) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Long-Press Revealed Control Bar (ONLY shown after holding empty screen area)
                AnimatedVisibility(
                    visible = uiState.isHomeControlBarOpen || uiState.isEditMode,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
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
                        onDone = onCloseControlBar
                    )
                }

                // Clean Portrait Bottom Floating Dock (App icons + optional icon-only Recents button)
                if (prefs.showDock && !prefs.cleanCanvasMode &&
                    (uiState.dockApps.isNotEmpty() || prefs.showRecentsInDock)
                ) {
                    PortraitFloatingDock(
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
}

@Composable
private fun HomeLayerPageContent(
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
    val columns = prefs.portraitColumns.coerceIn(3, 6)
    val minRows = 6

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
            isLandscape = false
        )
    }

    val activeDragPayload = dragController.activeDrag?.payload
    val draggedAppKey = (activeDragPayload as? HomeDragPayload.App)?.app?.componentKey
    val draggedNestId = (activeDragPayload as? HomeDragPayload.Nest)?.nest?.id

    // When clean (no widgets / big folders and <= 6 rows), fill entire canvas height so users
    // can freely drop icons in the top, middle, bottom, or corners and keep them locked there.
    if (pageWidgets.isEmpty() && bigNests.isEmpty() && allocation.totalRows <= minRows) {
        Column(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0 until allocation.totalRows) {
                PortraitGridSlotRow(
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
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User-added Widgets on this Layer
            itemsIndexed(
                items = pageWidgets,
                key = { _, item -> "p${pageIndex}_${item.id}" }
            ) { index, widget ->
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
                            onCustomize = { onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget)) },
                            onCycleSize = { onCycleWidgetSize(widget.id) },
                            onMoveUp = { if (index > 0) onMoveWidget(widget.id, -1) },
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
                            onCustomize = { onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget)) },
                            onCycleSize = { onCycleWidgetSize(widget.id) },
                            onMoveUp = { if (index > 0) onMoveWidget(widget.id, -1) },
                            onMoveDown = { onMoveWidget(widget.id, 1) },
                            onRemove = { onRemoveWidget(widget.id) }
                        )
                    }
                }
            }

            // Big Folders (2x1 / 2x2)
            itemsIndexed(
                items = bigNests,
                key = { _, nest -> "big_nest_${pageIndex}_${nest.id}" }
            ) { _, nest ->
                val isMergeTarget =
                    (hoveredDropTarget as? HomeDropTarget.AddToExistingNest)?.targetNestId == nest.id
                OwlNestTile(
                    nest = nest,
                    appsInNest = nest.appKeys.mapNotNull { uiState.findApp(it) },
                    globalIconShape = prefs.iconShape,
                    iconSize = prefs.iconSizeDp.dp,
                    showLabel = prefs.showAppLabels,
                    glassOpacity = prefs.glassOpacity,
                    isEditMode = uiState.isEditMode,
                    onClick = { onOpenNest(nest) },
                    onLongClick = { onCustomizeItem(ItemCustomizationTarget.NestTarget(nest)) },
                    onLaunchAppDirectly = onLaunchApp,
                    dragController = dragController,
                    pageIndex = pageIndex,
                    indexInPage = -1,
                    isMergeHighlighted = isMergeTarget
                )
            }

            // Free-Slot Grid Rows (empty slots remain droppable targets)
            items(
                count = allocation.totalRows,
                key = { row -> "p${pageIndex}_slotrow_$row" }
            ) { row ->
                PortraitGridSlotRow(
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
                        .height(86.dp)
                )
            }
        }
    }
}

@Composable
private fun PortraitGridSlotRow(
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        OwlNestTile(
                            nest = nest,
                            appsInNest = appsInNest,
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
                            dragPayload = HomeDragPayload.Nest(
                                nest = nest,
                                appsInNest = appsInNest,
                                sourcePage = pageIndex,
                                iconShape = prefs.iconShape,
                                iconSize = prefs.iconSizeDp.dp,
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
                        val itemSize = (prefs.iconSizeDp * placed.scale).coerceIn(36f, 84f).dp
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
                                onCustomizeItem(ItemCustomizationTarget.AppTarget(placed, app))
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
fun LayerDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { idx ->
            val selected = idx == currentPage
            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(if (selected) 20.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.35f)
                    )
            )
        }
    }
}

fun Modifier.layerTransitionModifier(
    style: LayerTransitionStyle,
    pageOffset: Float
): Modifier = graphicsLayer {
    val absOffset = abs(pageOffset).coerceIn(0f, 1f)
    when (style) {
        LayerTransitionStyle.SLIDE -> Unit
        LayerTransitionStyle.DEPTH_PARALLAX -> {
            val scale = 1f - absOffset * 0.08f
            scaleX = scale
            scaleY = scale
            alpha = 1f - absOffset * 0.25f
        }
        LayerTransitionStyle.FADE -> {
            alpha = 1f - absOffset * 0.65f
        }
        LayerTransitionStyle.TILT -> {
            rotationY = pageOffset * 14f
            alpha = 1f - absOffset * 0.2f
        }
    }
}

@Composable
private fun PortraitFloatingDock(
    dockApps: List<AppInfo>,
    uiState: LauncherUiState,
    showRecentsButton: Boolean,
    onOpenRecents: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit
) {
    val prefs = uiState.prefs
    val dockIconSize = (prefs.iconSizeDp - 4).coerceAtLeast(40).dp
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = (prefs.glassOpacity + 0.14f).coerceAtMost(0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("portrait_dock")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.take(5).forEach { app ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
            }

            if (showRecentsButton) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                            .testTag("dock_recents_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CropSquare,
                            contentDescription = stringResource(R.string.recents_desc),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
