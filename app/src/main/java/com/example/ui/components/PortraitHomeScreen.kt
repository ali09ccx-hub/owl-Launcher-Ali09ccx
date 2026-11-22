package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.FolderSizeMode
import com.example.model.GestureId
import com.example.model.LayerTransitionStyle
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
import com.example.model.PlacedWidget
import com.example.model.PlacedWidgetKind
import com.example.ui.ItemCustomizationTarget
import com.example.ui.LauncherUiState
import com.example.ui.theme.AmiriEditorialFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.widgets.OwlAppWidgetHost
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PortraitHomeScreen(
    uiState: LauncherUiState,
    pagerState: PagerState,
    widgetHost: OwlAppWidgetHost,
    onGesture: (GestureId) -> Unit,
    onLongPressHome: () -> Unit,
    onCloseControlBar: () -> Unit,
    onToggleEditMode: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: (Int) -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onOpenCreateNest: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onToggleNightOwl: () -> Unit,
    onToggleFocusPerch: () -> Unit,
    onToggleCleanCanvas: () -> Unit,
    onToggleSafetyShield: () -> Unit,
    onRequestSetDefaultLauncher: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onUnpinHomeApp: (AppInfo) -> Unit,
    onMoveAppStep: (String, Int) -> Unit,
    onMoveAppToPage: (String, Int) -> Unit,
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
    val now = rememberCurrentTimeMillis()
    val prefs = uiState.prefs
    val editionTitle = localizedEditionLabel(prefs.editorialCustomTitle, now)
    val timeString = formatLocalizedTime(now)
    val dateString = formatLocalizedDate(now)

    var isDraggingHomeItem by remember { mutableStateOf(false) }
    val pageListStates = remember { mutableStateMapOf<Int, LazyListState>() }

    OwlGestureOverlay(
        onGesture = onGesture,
        onLongPressEmptyArea = onLongPressHome,
        isChildDragging = isDraggingHomeItem,
        canScrollBackward = { pageListStates[pagerState.currentPage]?.canScrollBackward == true },
        canScrollForward = { pageListStates[pagerState.currentPage]?.canScrollForward == true },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Minimal, clean Portrait Glance Header (No permanent Edit/Settings clutter; revealed via long press!)
            PortraitCleanHeader(
                timeString = timeString,
                dateString = dateString,
                editionTitle = editionTitle,
                uiState = uiState,
                onToggleNightOwl = onToggleNightOwl,
                onToggleFocusPerch = onToggleFocusPerch,
                onToggleSafetyShield = onToggleSafetyShield
            )

            AnimatedVisibility(
                visible = !prefs.cleanCanvasMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Multi-Layer Horizontal Pager
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isDraggingHomeItem,
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
                            onOpenDrawer = onOpenDrawer,
                            onOpenWidgetPicker = onOpenWidgetPicker,
                            onRequestSetDefaultLauncher = onRequestSetDefaultLauncher,
                            onLaunchApp = onLaunchApp,
                            onShowAppMenu = onShowAppMenu,
                            onUnpinHomeApp = onUnpinHomeApp,
                            onMoveAppStep = onMoveAppStep,
                            onMoveAppToPage = onMoveAppToPage,
                            onDragActiveChanged = { isDraggingHomeItem = it },
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

                    // Layer Page Indicator Dots
                    if (prefs.pageCount > 1) {
                        LayerDotsIndicator(
                            pageCount = prefs.pageCount,
                            currentPage = pagerState.currentPage,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            if (prefs.cleanCanvasMode) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        onClick = onToggleCleanCanvas,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clean_mode_on),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Long-Press Revealed Control Bar (Pencil Edit, Settings, Widgets, Gestures, Layers)
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

            // Floating Glass Dock
            if (prefs.showDock && !prefs.cleanCanvasMode) {
                PortraitFloatingDock(
                    dockApps = uiState.dockApps,
                    uiState = uiState,
                    onLaunchApp = onLaunchApp,
                    onShowAppMenu = onShowAppMenu,
                    onOpenDrawer = onOpenDrawer
                )
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
    onOpenDrawer: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onRequestSetDefaultLauncher: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onUnpinHomeApp: (AppInfo) -> Unit,
    onMoveAppStep: (String, Int) -> Unit,
    onMoveAppToPage: (String, Int) -> Unit,
    onDragActiveChanged: (Boolean) -> Unit,
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageWidgets = uiState.widgetsForPage(pageIndex)
    val pageNests = uiState.nestsForPage(pageIndex)
    val pageApps = uiState.placedAppsForPage(pageIndex)
    val columns = prefs.portraitColumns.coerceIn(3, 6)

    val smallNests = pageNests.filter { it.folderSize == FolderSizeMode.SIZE_1X1 }
    val bigNests = pageNests.filter { it.folderSize != FolderSizeMode.SIZE_1X1 }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (prefs.safetyShieldActive && pageIndex == 0) {
            item(key = "safety_banner_$pageIndex") {
                Surface(
                    onClick = {},
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.safety_shield_active_msg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (!uiState.isDefaultLauncher && pageIndex == 0 && !prefs.safetyShieldActive) {
            item(key = "default_launcher_card") {
                DefaultLauncherCompactCard(
                    glassOpacity = prefs.glassOpacity,
                    onSetDefault = onRequestSetDefaultLauncher
                )
            }
        }

        if (prefs.focusPerchActive && pageIndex == 0) {
            item(key = "focus_banner") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CenterFocusStrong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.focus_active_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Widgets on this Layer
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
                onLaunchAppDirectly = onLaunchApp
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until columns) {
                        val entry = rowItems.getOrNull(col)
                        Box(modifier = Modifier.weight(1f)) {
                            when (entry) {
                                is HomeGridEntry.NestEntry -> {
                                    OwlNestTile(
                                        nest = entry.nest,
                                        appsInNest = entry.nest.appKeys.mapNotNull { uiState.findApp(it) },
                                        globalIconShape = prefs.iconShape,
                                        iconSize = prefs.iconSizeDp.dp,
                                        showLabel = prefs.showAppLabels,
                                        glassOpacity = prefs.glassOpacity,
                                        isEditMode = uiState.isEditMode,
                                        onClick = { onOpenNest(entry.nest) },
                                        onLongClick = {
                                            onCustomizeItem(ItemCustomizationTarget.NestTarget(entry.nest))
                                        },
                                        onLaunchAppDirectly = onLaunchApp
                                    )
                                }
                                is HomeGridEntry.AppEntry -> {
                                    val placed = entry.placed
                                    val app = entry.app
                                    val itemSize = (prefs.iconSizeDp * placed.scale).coerceIn(36f, 84f).dp
                                    AppIconTile(
                                        app = app,
                                        iconShape = placed.shapeOverride ?: prefs.iconShape,
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
                                        onDragActiveChanged = onDragActiveChanged,
                                        onDragFinished = { offset, tileSize ->
                                            val cellW = tileSize.width.toFloat().coerceAtLeast(90f)
                                            val cellH = tileSize.height.toFloat().coerceAtLeast(100f)
                                            val signedDx = if (isRtl) -offset.x else offset.x
                                            val edgeThreshold = cellW * (columns - 0.15f)
                                            when {
                                                signedDx > edgeThreshold && pageIndex < prefs.pageCount - 1 -> {
                                                    onMoveAppToPage(app.componentKey, pageIndex + 1)
                                                }
                                                signedDx < -edgeThreshold && pageIndex > 0 -> {
                                                    onMoveAppToPage(app.componentKey, pageIndex - 1)
                                                }
                                                else -> {
                                                    val colDelta = (signedDx / cellW).roundToInt()
                                                    val rowDelta = (offset.y / cellH).roundToInt()
                                                    val stepDelta = rowDelta * columns + colDelta
                                                    if (stepDelta != 0) {
                                                        onMoveAppStep(app.componentKey, stepDelta)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                null -> Spacer(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        } else if (pageWidgets.isEmpty() && bigNests.isEmpty() && !prefs.safetyShieldActive) {
            item(key = "empty_home_hint_$pageIndex") {
                EmptyHomeHintCard(
                    glassOpacity = prefs.glassOpacity,
                    onOpenDrawer = onOpenDrawer,
                    onOpenWidgetPicker = onOpenWidgetPicker
                )
            }
        }

        item(key = "bottom_hint_$pageIndex") {
            Text(
                text = stringResource(R.string.long_press_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}

sealed interface HomeGridEntry {
    data class NestEntry(val nest: OwlNest) : HomeGridEntry
    data class AppEntry(val placed: PlacedAppItem, val app: AppInfo) : HomeGridEntry
}

@Composable
private fun PortraitCleanHeader(
    timeString: String,
    dateString: String,
    editionTitle: String,
    uiState: LauncherUiState,
    onToggleNightOwl: () -> Unit,
    onToggleFocusPerch: () -> Unit,
    onToggleSafetyShield: () -> Unit
) {
    val prefs = uiState.prefs
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = editionTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = timeString,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
        Text(
            text = dateString,
            fontFamily = AmiriEditorialFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderStatusChip(
                active = prefs.nightOwlMode,
                icon = Icons.Filled.Bedtime,
                label = stringResource(R.string.night_owl_mode),
                onClick = onToggleNightOwl,
                testTag = "chip_night_owl"
            )
            HeaderStatusChip(
                active = prefs.focusPerchActive,
                icon = Icons.Filled.CenterFocusStrong,
                label = stringResource(R.string.focus_perch_mode),
                onClick = onToggleFocusPerch,
                testTag = "chip_focus_perch"
            )
            HeaderStatusChip(
                active = prefs.safetyShieldActive,
                icon = Icons.Filled.Shield,
                label = stringResource(R.string.safety_shield_mode),
                onClick = onToggleSafetyShield,
                testTag = "chip_safety_shield"
            )
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
fun HeaderStatusChip(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.50f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DefaultLauncherCompactCard(
    glassOpacity: Float,
    onSetDefault: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = (glassOpacity + 0.14f).coerceAtMost(0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.set_default_launcher_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.set_default_launcher_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onSetDefault,
                modifier = Modifier.testTag("set_default_launcher_btn")
            ) {
                Text(text = stringResource(R.string.set_default_launcher_btn))
            }
        }
    }
}

@Composable
fun EmptyHomeHintCard(
    glassOpacity: Float,
    onOpenDrawer: () -> Unit,
    onOpenWidgetPicker: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = glassOpacity),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("empty_open_drawer_btn")
                ) {
                    Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.pin_popular_apps))
                }
                OutlinedButton(
                    onClick = onOpenWidgetPicker,
                    modifier = Modifier.testTag("empty_add_widget_btn")
                ) {
                    Text(stringResource(R.string.add_first_widget))
                }
            }
        }
    }
}

@Composable
private fun PortraitFloatingDock(
    dockApps: List<AppInfo>,
    uiState: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val prefs = uiState.prefs
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = (prefs.glassOpacity + 0.14f).coerceAtMost(0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
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

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenDrawer)
                    .testTag("dock_open_drawer_btn"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_owl_launcher_icon),
                        contentDescription = stringResource(R.string.open_drawer_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
