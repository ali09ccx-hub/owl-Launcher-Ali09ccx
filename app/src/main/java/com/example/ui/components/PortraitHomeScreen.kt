package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.example.model.AppInfo
import com.example.model.FolderSizeMode
import com.example.model.GestureId
import com.example.model.LayerTransitionStyle
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
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
                    is HomeDropTarget.ReorderAppSlot -> {
                        onReorderOrMoveApp(
                            payload.app.componentKey,
                            target.targetPage,
                            target.targetIndex
                        )
                    }
                    is HomeDropTarget.ReorderNestSlot -> {
                        onReorderOrMoveApp(payload.app.componentKey, pagerState.currentPage, null)
                    }
                }
                is HomeDragPayload.Nest -> when (target) {
                    is HomeDropTarget.ReorderNestSlot -> {
                        onReorderOrMoveNest(payload.nest.id, target.targetPage, target.targetIndex)
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
                        onOpenGestures = { onOpenSettings(3) },
                        onAddPage = onAddPage,
                        onRemovePage = onRemovePage,
                        onDone = onCloseControlBar
                    )
                }

                // Clean Portrait Bottom Floating Dock (App icons only, no Owl quick-menu button)
                if (prefs.showDock && !prefs.cleanCanvasMode && uiState.dockApps.isNotEmpty()) {
                    PortraitFloatingDock(
                        dockApps = uiState.dockApps,
                        uiState = uiState,
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

    val smallNests = pageNests.filter { it.folderSize == FolderSizeMode.SIZE_1X1 }
    val bigNests = pageNests.filter { it.folderSize != FolderSizeMode.SIZE_1X1 }

    val smallNestIndexById = remember(smallNests) {
        smallNests.mapIndexed { idx, nest -> nest.id to idx }.toMap()
    }
    val appIndexByKey = remember(pageApps) {
        pageApps.mapIndexed { idx, (placed, _) -> placed.componentKey to idx }.toMap()
    }

    val activeDragPayload = dragController.activeDrag?.payload
    val draggedAppKey = (activeDragPayload as? HomeDragPayload.App)?.app?.componentKey
    val draggedNestId = (activeDragPayload as? HomeDragPayload.Nest)?.nest?.id

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                isMergeHighlighted = isMergeTarget
            )
        }

        // 1x1 Folders + Pinned Apps Grid
        val gridEntries = buildList<HomeGridEntry> {
            smallNests.forEach { add(HomeGridEntry.NestEntry(it)) }
            pageApps.forEach { (placed, app) -> add(HomeGridEntry.AppEntry(placed, app)) }
        }

        if (gridEntries.isNotEmpty()) {
            val rows = gridEntries.chunked(columns)
            itemsIndexed(
                items = rows,
                key = { idx, _ -> "p${pageIndex}_row_$idx" }
            ) { _, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until columns) {
                        val entry = rowItems.getOrNull(col)
                        Box(
                            modifier = Modifier.weight(1f),
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
                                        indexInPage = smallNestIndexById[nest.id] ?: 0,
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
                                        indexInPage = appIndexByKey[app.componentKey] ?: 0,
                                        isMergeHighlighted = isMergeTarget,
                                        isHiddenWhileDragged = draggedAppKey == app.componentKey
                                    )
                                }

                                null -> Spacer(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface HomeGridEntry {
    data class NestEntry(val nest: OwlNest) : HomeGridEntry
    data class AppEntry(val placed: PlacedAppItem, val app: AppInfo) : HomeGridEntry
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
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit
) {
    val prefs = uiState.prefs
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
                        iconSize = (prefs.iconSizeDp - 4).coerceAtLeast(40).dp,
                        showFrameBackdrop = prefs.showIconFrameBackdrop,
                        showLabel = false,
                        isEditMode = false,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onShowAppMenu(app) }
                    )
                }
            }
        }
    }
}
