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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.SpaceGroteskFamily
import com.example.widgets.OwlAppWidgetHost
import kotlin.math.roundToInt

@Composable
fun LandscapeEditorialHomeScreen(
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

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
        if (prefs.cleanCanvasMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    onClick = onToggleCleanCanvas,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clean_mode_on),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            return@OwlGestureOverlay
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT COLUMN: Clean Editorial Masthead, Lead Dispatch & Quick Dock
                Surface(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .testTag("landscape_editorial_lead"),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = prefs.glassOpacity),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = editionTitle.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = timeString,
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White
                            )
                            Text(
                                text = dateString,
                                fontFamily = AmiriEditorialFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HeaderStatusChip(
                                    active = prefs.nightOwlMode,
                                    icon = Icons.Filled.Bedtime,
                                    label = stringResource(R.string.night_owl_mode),
                                    onClick = onToggleNightOwl,
                                    testTag = "landscape_night_owl"
                                )
                                HeaderStatusChip(
                                    active = prefs.focusPerchActive,
                                    icon = Icons.Filled.CenterFocusStrong,
                                    label = stringResource(R.string.focus_perch_mode),
                                    onClick = onToggleFocusPerch,
                                    testTag = "landscape_focus_perch"
                                )
                                HeaderStatusChip(
                                    active = prefs.safetyShieldActive,
                                    icon = Icons.Filled.Shield,
                                    label = stringResource(R.string.safety_shield_mode),
                                    onClick = onToggleSafetyShield,
                                    testTag = "landscape_safety_shield"
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!prefs.safetyShieldActive) {
                                Surface(
                                    onClick = onOpenNoteEditor,
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.EditNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.owl_widget_note_title),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = prefs.dispatchNoteText.ifBlank {
                                                stringResource(R.string.note_placeholder)
                                            },
                                            fontFamily = AmiriEditorialFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (prefs.dispatchNoteText.isBlank()) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (prefs.showDock) {
                            Column {
                                EditorialSectionHeader(title = stringResource(R.string.editorial_section_quick))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    uiState.dockApps.take(4).forEach { app ->
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                            AppIconTile(
                                                app = app,
                                                iconShape = prefs.iconShape,
                                                iconSize = 40.dp,
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
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onOpenDrawer)
                                            .testTag("landscape_drawer_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(44.dp)
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
                    }
                }

                // RIGHT COLUMN: Multi-Layer Magazine Spread
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isDraggingHomeItem,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("landscape_editorial_spread")
                    ) { pageIndex ->
                        val pageOffset = (pagerState.currentPage - pageIndex) +
                            pagerState.currentPageOffsetFraction
                        val listState = pageListStates.getOrPut(pageIndex) { LazyListState() }

                        val pageWidgets = uiState.widgetsForPage(pageIndex)
                        val pageNests = uiState.nestsForPage(pageIndex)
                        val pageApps = uiState.placedAppsForPage(pageIndex)
                        val columns = prefs.landscapeColumns.coerceIn(3, 6)
                        val smallNests = pageNests.filter { it.folderSize == FolderSizeMode.SIZE_1X1 }
                        val bigNests = pageNests.filter { it.folderSize != FolderSizeMode.SIZE_1X1 }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .layerTransitionModifier(prefs.layerTransition, pageOffset),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (prefs.safetyShieldActive && pageIndex == 0) {
                                item(key = "land_safety") {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.safety_shield_active_msg),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            }

                            if (!uiState.isDefaultLauncher && pageIndex == 0 && !prefs.safetyShieldActive) {
                                item(key = "landscape_default_launcher") {
                                    DefaultLauncherCompactCard(
                                        glassOpacity = prefs.glassOpacity,
                                        onSetDefault = onRequestSetDefaultLauncher
                                    )
                                }
                            }

                            if (pageWidgets.isNotEmpty()) {
                                item(key = "sec_widgets_$pageIndex") {
                                    EditorialSectionHeader(title = stringResource(R.string.editorial_section_widgets))
                                }
                                itemsIndexed(
                                    items = pageWidgets,
                                    key = { _, widget -> "land_${pageIndex}_${widget.id}" }
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
                                                onCustomize = {
                                                    onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
                                                },
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
                                                onCustomize = {
                                                    onCustomizeItem(ItemCustomizationTarget.WidgetTarget(widget))
                                                },
                                                onCycleSize = { onCycleWidgetSize(widget.id) },
                                                onMoveUp = { if (index > 0) onMoveWidget(widget.id, -1) },
                                                onMoveDown = { onMoveWidget(widget.id, 1) },
                                                onRemove = { onRemoveWidget(widget.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            if (pageNests.isNotEmpty()) {
                                item(key = "sec_nests_$pageIndex") {
                                    EditorialSectionHeader(title = stringResource(R.string.editorial_section_nests))
                                }
                                itemsIndexed(
                                    items = bigNests,
                                    key = { _, nest -> "land_bignest_${pageIndex}_${nest.id}" }
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
                                        onLongClick = {
                                            onCustomizeItem(ItemCustomizationTarget.NestTarget(nest))
                                        },
                                        onLaunchAppDirectly = onLaunchApp
                                    )
                                }
                                val nestRows = smallNests.chunked(columns)
                                itemsIndexed(
                                    items = nestRows,
                                    key = { idx, _ -> "land_nest_row_${pageIndex}_$idx" }
                                ) { _, rowNests ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (col in 0 until columns) {
                                            val nest = rowNests.getOrNull(col)
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (nest != null) {
                                                    OwlNestTile(
                                                        nest = nest,
                                                        appsInNest = nest.appKeys.mapNotNull { uiState.findApp(it) },
                                                        globalIconShape = prefs.iconShape,
                                                        iconSize = (prefs.iconSizeDp - 4).coerceAtLeast(40).dp,
                                                        showLabel = prefs.showAppLabels,
                                                        glassOpacity = prefs.glassOpacity,
                                                        isEditMode = uiState.isEditMode,
                                                        onClick = { onOpenNest(nest) },
                                                        onLongClick = {
                                                            onCustomizeItem(ItemCustomizationTarget.NestTarget(nest))
                                                        },
                                                        onLaunchAppDirectly = onLaunchApp
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.fillMaxWidth())
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "sec_apps_$pageIndex") {
                                EditorialSectionHeader(title = stringResource(R.string.editorial_section_apps))
                            }

                            if (pageApps.isNotEmpty()) {
                                val appRows = pageApps.chunked(columns)
                                itemsIndexed(
                                    items = appRows,
                                    key = { idx, _ -> "land_app_row_${pageIndex}_$idx" }
                                ) { _, rowApps ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (col in 0 until columns) {
                                            val pair = rowApps.getOrNull(col)
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (pair != null) {
                                                    val (placed, app) = pair
                                                    val itemSize = ((prefs.iconSizeDp - 2) * placed.scale)
                                                        .coerceIn(36f, 80f).dp
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
                                                            onCustomizeItem(
                                                                ItemCustomizationTarget.AppTarget(placed, app)
                                                            )
                                                        },
                                                        onRemoveInEditMode = { onUnpinHomeApp(app) },
                                                        onDragActiveChanged = { isDraggingHomeItem = it },
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
                                                } else {
                                                    Spacer(modifier = Modifier.fillMaxWidth())
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (!prefs.safetyShieldActive) {
                                item(key = "land_empty_apps_$pageIndex") {
                                    EmptyHomeHintCard(
                                        glassOpacity = prefs.glassOpacity,
                                        onOpenDrawer = onOpenDrawer,
                                        onOpenWidgetPicker = onOpenWidgetPicker
                                    )
                                }
                            }
                        }
                    }

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
                    onOpenGestures = { onOpenSettings(3) },
                    onAddPage = onAddPage,
                    onRemovePage = onRemovePage,
                    onDone = onCloseControlBar
                )
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
