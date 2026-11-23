package com.example

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ItemCustomizationTarget
import com.example.ui.LauncherUiEvent
import com.example.ui.LauncherViewModel
import com.example.ui.components.AddToNestDialog
import com.example.ui.components.AppContextActionDialog
import com.example.ui.components.AppDrawerSheet
import com.example.ui.components.CreateNestDialog
import com.example.ui.components.DispatchNoteEditorDialog
import com.example.ui.components.ExpandedFolderOverlay
import com.example.ui.components.ItemCustomizationDialog
import com.example.ui.components.LandscapeEditorialHomeScreen
import com.example.ui.components.PortraitHomeScreen
import com.example.ui.components.RecentsOverlay
import com.example.ui.components.SettingsStudioSheet
import com.example.ui.components.WallpaperCanvas
import com.example.ui.components.WidgetPickerSheet
import com.example.ui.theme.OwlLauncherTheme
import com.example.widgets.OwlAppWidgetHost
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            val defaultLauncherResult = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshSystemState()
            }

            val bindWidgetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                viewModel.onBindWidgetResult(result.resultCode)
            }

            OwlLauncherTheme(
                accentTheme = uiState.prefs.accentTheme,
                nightOwlMode = uiState.prefs.nightOwlMode
            ) {
                OwlLauncherRoot(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onRequestSetDefaultLauncher = {
                        val intent = viewModel.appsManager.createSetDefaultLauncherIntent()
                        defaultLauncherResult.launch(intent)
                    },
                    onLaunchBindWidget = { intent ->
                        bindWidgetLauncher.launch(intent)
                    },
                    onConfigureAppWidget = { widgetId ->
                        viewModel.widgetHost.startConfigureActivity(this@MainActivity, widgetId)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.widgetHost.startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemState()
    }

    override fun onStop() {
        super.onStop()
        viewModel.widgetHost.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            viewModel.closeAllOverlays()
        }
    }

    @Deprecated("Required by AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OwlAppWidgetHost.REQUEST_CONFIGURE_APPWIDGET) {
            viewModel.onConfigureWidgetResult(resultCode)
        }
    }
}

@Composable
private fun OwlLauncherRoot(
    viewModel: LauncherViewModel,
    snackbarHostState: SnackbarHostState,
    onRequestSetDefaultLauncher: () -> Unit,
    onLaunchBindWidget: (Intent) -> Unit,
    onConfigureAppWidget: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val systemIsLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val effectiveWallpaperLandscape = uiState.settingsPreviewLandscape ?: systemIsLandscape
    val scope = rememberCoroutineScope()

    val pageCount = uiState.prefs.pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    LaunchedEffect(uiState.isDefaultLauncher, uiState.prefs.syncSystemWallpaper) {
        if (uiState.isDefaultLauncher && uiState.prefs.syncSystemWallpaper) {
            viewModel.syncWallpaperIfNeeded(isLandscape = systemIsLandscape)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LauncherUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LauncherUiEvent.LaunchBindWidgetPermission -> {
                    onLaunchBindWidget(event.intent)
                }
                is LauncherUiEvent.ConfigureAppWidget -> {
                    onConfigureAppWidget(event.appWidgetId)
                }
                is LauncherUiEvent.ScrollToPage -> {
                    scope.launch {
                        val safePage = event.pageIndex.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
                        pagerState.animateScrollToPage(safePage)
                    }
                }
            }
        }
    }

    val pageParallaxFraction by remember(pageCount) {
        derivedStateOf {
            if (pageCount <= 1) 0f
            else (pagerState.currentPage + pagerState.currentPageOffsetFraction) / (pageCount - 1).toFloat() - 0.5f
        }
    }

    // Keep Home Screen anchored when acting as default launcher
    BackHandler(enabled = true) {
        viewModel.handleBackPress()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Dual Portrait / Landscape Wallpaper Canvas + Living Owl Storm Engine
            val wallpaperPaused = uiState.isDrawerOpen ||
                uiState.isRecentsOpen ||
                uiState.isSettingsOpen ||
                uiState.isWidgetPickerOpen ||
                uiState.selectedNestToOpen != null
            WallpaperCanvas(
                isLandscape = effectiveWallpaperLandscape,
                portraitConfig = uiState.prefs.portraitWallpaper,
                landscapeConfig = uiState.prefs.landscapeWallpaper,
                nightOwlMode = uiState.prefs.nightOwlMode,
                stormMode = uiState.prefs.stormMode,
                rainDensity = uiState.prefs.rainDensity,
                lightningEnabled = uiState.prefs.lightningEnabled,
                stormFogEnabled = uiState.prefs.stormFogEnabled,
                pageParallaxFraction = pageParallaxFraction,
                isPaused = wallpaperPaused
            )

            // Responsive Orientation Home Layout with Multi-Page Horizontal Layers & 24 Gestures
            if (systemIsLandscape) {
                LandscapeEditorialHomeScreen(
                    uiState = uiState,
                    pagerState = pagerState,
                    widgetHost = viewModel.widgetHost,
                    onGesture = { viewModel.executeGesture(it) },
                    onOpenRecents = { viewModel.openRecents(true) },
                    onLongPressHome = { viewModel.onHomeBackgroundLongPress() },
                    onCloseControlBar = { viewModel.closeHomeControlBarAndEdit() },
                    onToggleEditMode = { viewModel.setEditMode(!uiState.isEditMode) },
                    onOpenSettings = { tab -> viewModel.openSettings(true, tab) },
                    onOpenWidgetPicker = { viewModel.openWidgetPicker(true) },
                    onOpenCreateNest = { viewModel.openCreateNestDialog(true) },
                    onAddPage = { viewModel.addHomePage() },
                    onRemovePage = { viewModel.removeCurrentHomePage() },
                    onJumpToPage = { page -> viewModel.jumpToPage(page) },
                    onLaunchApp = { viewModel.launchApp(it) },
                    onShowAppMenu = { viewModel.showAppMenu(it) },
                    onUnpinHomeApp = { viewModel.togglePinHome(it) },
                    onReorderOrMoveApp = { key, page, idx ->
                        viewModel.reorderOrMovePlacedApp(key, page, idx, isLandscape = true)
                    },
                    onMergeAppsIntoFolder = { draggedKey, targetKey, page ->
                        viewModel.mergeAppsIntoFolder(draggedKey, targetKey, page)
                    },
                    onDropAppIntoNest = { draggedKey, nestId ->
                        viewModel.dropAppIntoNest(draggedKey, nestId)
                    },
                    onReorderOrMoveNest = { nestId, page, idx ->
                        viewModel.reorderOrMoveNest(nestId, page, idx, isLandscape = true)
                    },
                    onCustomizeItem = { viewModel.openItemCustomization(it) },
                    onOpenNest = { viewModel.openNest(it) },
                    onToggleFocusTimer = { viewModel.toggleFocusTimer() },
                    onResetFocusTimer = { viewModel.resetFocusTimer() },
                    onToggleFlashlight = { viewModel.toggleFlashlight() },
                    onOpenNoteEditor = { viewModel.openNoteEditor(true) },
                    onCycleWisdom = { viewModel.cycleWisdomQuote() },
                    onPickGifForWidget = { id, uri -> viewModel.attachGifToWidget(id, uri) },
                    onCycleWidgetSize = { viewModel.cycleWidgetSize(it) },
                    onMoveWidget = { id, dir -> viewModel.moveWidget(id, dir) },
                    onRemoveWidget = { viewModel.removeWidget(it) }
                )
            } else {
                PortraitHomeScreen(
                    uiState = uiState,
                    pagerState = pagerState,
                    widgetHost = viewModel.widgetHost,
                    onGesture = { viewModel.executeGesture(it) },
                    onOpenRecents = { viewModel.openRecents(true) },
                    onLongPressHome = { viewModel.onHomeBackgroundLongPress() },
                    onCloseControlBar = { viewModel.closeHomeControlBarAndEdit() },
                    onToggleEditMode = { viewModel.setEditMode(!uiState.isEditMode) },
                    onOpenSettings = { tab -> viewModel.openSettings(true, tab) },
                    onOpenWidgetPicker = { viewModel.openWidgetPicker(true) },
                    onOpenCreateNest = { viewModel.openCreateNestDialog(true) },
                    onAddPage = { viewModel.addHomePage() },
                    onRemovePage = { viewModel.removeCurrentHomePage() },
                    onJumpToPage = { page -> viewModel.jumpToPage(page) },
                    onLaunchApp = { viewModel.launchApp(it) },
                    onShowAppMenu = { viewModel.showAppMenu(it) },
                    onUnpinHomeApp = { viewModel.togglePinHome(it) },
                    onReorderOrMoveApp = { key, page, idx ->
                        viewModel.reorderOrMovePlacedApp(key, page, idx, isLandscape = false)
                    },
                    onMergeAppsIntoFolder = { draggedKey, targetKey, page ->
                        viewModel.mergeAppsIntoFolder(draggedKey, targetKey, page)
                    },
                    onDropAppIntoNest = { draggedKey, nestId ->
                        viewModel.dropAppIntoNest(draggedKey, nestId)
                    },
                    onReorderOrMoveNest = { nestId, page, idx ->
                        viewModel.reorderOrMoveNest(nestId, page, idx, isLandscape = false)
                    },
                    onCustomizeItem = { viewModel.openItemCustomization(it) },
                    onOpenNest = { viewModel.openNest(it) },
                    onToggleFocusTimer = { viewModel.toggleFocusTimer() },
                    onResetFocusTimer = { viewModel.resetFocusTimer() },
                    onToggleFlashlight = { viewModel.toggleFlashlight() },
                    onOpenNoteEditor = { viewModel.openNoteEditor(true) },
                    onCycleWisdom = { viewModel.cycleWisdomQuote() },
                    onPickGifForWidget = { id, uri -> viewModel.attachGifToWidget(id, uri) },
                    onCycleWidgetSize = { viewModel.cycleWidgetSize(it) },
                    onMoveWidget = { id, dir -> viewModel.moveWidget(id, dir) },
                    onRemoveWidget = { viewModel.removeWidget(it) }
                )
            }

            // Recent Apps Snapshot Switcher ("اللقطات")
            AnimatedVisibility(
                visible = uiState.isRecentsOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f)
            ) {
                RecentsOverlay(
                    uiState = uiState,
                    onDismiss = { viewModel.openRecents(false) },
                    onLaunchApp = { viewModel.launchApp(it) },
                    onDismissRecent = { viewModel.dismissRecentApp(it) },
                    onClearAll = { viewModel.clearRecentApps() },
                    onOpenAppInfo = { viewModel.appsManager.openAppInfo(it.packageName) }
                )
            }

            // App Drawer Overlay (Swipe Down or Tap X to return Home)
            AnimatedVisibility(
                visible = uiState.isDrawerOpen,
                enter = slideInVertically { it / 3 } + fadeIn(),
                exit = slideOutVertically { it / 3 } + fadeOut()
            ) {
                AppDrawerSheet(
                    uiState = uiState,
                    onDismiss = { viewModel.openDrawer(false) },
                    onOpenRecents = { viewModel.openRecents(true) },
                    onLaunchApp = { viewModel.launchApp(it) },
                    onShowAppMenu = { viewModel.showAppMenu(it) },
                    onVaultUnlocked = { viewModel.setVaultUnlocked(it) }
                )
            }

            // Widget Picker Overlay
            AnimatedVisibility(
                visible = uiState.isWidgetPickerOpen,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut()
            ) {
                WidgetPickerSheet(
                    uiState = uiState,
                    onDismiss = { viewModel.openWidgetPicker(false) },
                    onAddOwlNativeWidget = { viewModel.addOwlNativeWidget(it) },
                    onAddSystemWidget = { viewModel.requestAddSystemWidget(it) }
                )
            }

            // Settings Studio Overlay
            AnimatedVisibility(
                visible = uiState.isSettingsOpen,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut()
            ) {
                SettingsStudioSheet(
                    uiState = uiState,
                    initialTab = uiState.initialSettingsTab,
                    systemIsLandscape = systemIsLandscape,
                    onDismiss = { viewModel.openSettings(false) },
                    onSetPreviewLandscape = { viewModel.setSettingsPreviewLandscape(it) },
                    onRequestSetDefaultLauncher = onRequestSetDefaultLauncher,
                    onPickPortraitWallpaper = { viewModel.saveWallpaperFromUri(false, it) },
                    onPickLandscapeWallpaper = { viewModel.saveWallpaperFromUri(true, it) },
                    onResetWallpaper = { viewModel.resetWallpaper(it) },
                    onUpdateWallpaperConfig = { land, cfg ->
                        viewModel.updateWallpaperConfig(land, cfg)
                    },
                    onSetSyncSystemWallpaper = { enabled ->
                        viewModel.setSyncSystemWallpaper(enabled, systemIsLandscape)
                    },
                    onApplySystemWallpaperNow = { land ->
                        viewModel.applyWallpaperToSystemNow(land)
                    },
                    onUpdateStormConfig = { mode, density, lightning, fog ->
                        viewModel.updateStormConfig(mode, density, lightning, fog)
                    },
                    onUpdateLayerConfig = { transition, showDock, showRecentsInDock ->
                        viewModel.updateLayerConfig(transition, showDock, showRecentsInDock)
                    },
                    onAddPage = { viewModel.addHomePage() },
                    onRemovePage = { viewModel.removeCurrentHomePage() },
                    onUpdateIconStyle = { shape, size, backdrop, labels, pCol, lCol ->
                        viewModel.updateIconStyle(shape, size, backdrop, labels, pCol, lCol)
                    },
                    onUpdateAppearance = { theme, glass, lock, masthead ->
                        viewModel.updateAppearance(theme, glass, lock, masthead, null)
                    },
                    onUpdateGestureBinding = { gId, binding ->
                        viewModel.updateGestureBinding(gId, binding)
                    },
                    onOpenCreateNest = { viewModel.openCreateNestDialog(true) },
                    onOpenWidgetPicker = { viewModel.openWidgetPicker(true) }
                )
            }

            // Context Menu for Any App
            uiState.selectedAppForMenu?.let { app ->
                AppContextActionDialog(
                    app = app,
                    uiState = uiState,
                    onDismiss = { viewModel.showAppMenu(null) },
                    onLaunch = { viewModel.launchApp(app) },
                    onTogglePinHome = { viewModel.togglePinHome(app) },
                    onCustomizePlacedApp = { placed ->
                        viewModel.openItemCustomization(ItemCustomizationTarget.AppTarget(placed, app))
                    },
                    onTogglePinDock = { viewModel.togglePinDock(app) },
                    onAddToNest = { viewModel.openAddToNestPicker(app) },
                    onToggleFocusEssential = { viewModel.toggleFocusEssential(app) },
                    onToggleVault = { viewModel.toggleHideInVault(app) },
                    onOpenInfo = { viewModel.appsManager.openAppInfo(app.packageName) },
                    onUninstall = { viewModel.appsManager.uninstallApp(app.packageName) }
                )
            }

            // Per-Item Customization Dialog (App / Folder / Widget Size, Shape, GIF, Layer)
            uiState.itemCustomizationTarget?.let { target ->
                ItemCustomizationDialog(
                    target = target,
                    pageCount = pageCount,
                    onDismiss = { viewModel.openItemCustomization(null) },
                    onUpdateApp = { viewModel.updatePlacedAppItem(it) },
                    onRemoveApp = { viewModel.togglePinHome(it) },
                    onUpdateNest = { viewModel.updateNest(it) },
                    onAttachGifToNest = { nestId, uri -> viewModel.attachGifToNest(nestId, uri) },
                    onDeleteNest = { viewModel.deleteNest(it) },
                    onUpdateWidget = { viewModel.updateWidget(it) },
                    onAttachGifToWidget = { widgetId, uri -> viewModel.attachGifToWidget(widgetId, uri) },
                    onRemoveWidget = { viewModel.removeWidget(it) }
                )
            }

            // Expanded Folder Page Overlay (Grid + Drag to Reorder / Extract to Home / Remove)
            var lastVisibleNest by remember { mutableStateOf(uiState.selectedNestToOpen) }
            if (uiState.selectedNestToOpen != null) {
                lastVisibleNest = uiState.selectedNestToOpen
            }
            AnimatedVisibility(
                visible = uiState.selectedNestToOpen != null,
                enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.88f),
                exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.88f)
            ) {
                lastVisibleNest?.let { nest ->
                    ExpandedFolderOverlay(
                        nest = nest,
                        uiState = uiState,
                        onDismiss = { viewModel.openNest(null) },
                        onRenameNest = { newName ->
                            viewModel.updateNest(nest.copy(name = newName))
                        },
                        onLaunchApp = { viewModel.launchApp(it) },
                        onShowAppMenu = { viewModel.showAppMenu(it) },
                        onReorderAppInNest = { appKey, targetIdx ->
                            viewModel.reorderAppInNest(nest.id, appKey, targetIdx)
                        },
                        onExtractAppToHome = { appKey ->
                            viewModel.extractAppFromNestToHome(nest.id, appKey)
                        },
                        onRemoveAppFromNest = { appKey ->
                            viewModel.removeAppFromNest(nest.id, appKey)
                        },
                        onCustomizeNest = {
                            viewModel.openItemCustomization(ItemCustomizationTarget.NestTarget(nest))
                        },
                        onDeleteNest = { viewModel.deleteNest(nest.id) }
                    )
                }
            }

            uiState.appToAddToNest?.let { app ->
                AddToNestDialog(
                    app = app,
                    nests = uiState.prefs.owlNests,
                    onDismiss = { viewModel.openAddToNestPicker(null) },
                    onSelectNest = { nestId -> viewModel.addAppToNest(nestId, app) },
                    onCreateNewNest = { name -> viewModel.createNest(name, app) }
                )
            }

            if (uiState.isCreateNestDialogOpen) {
                CreateNestDialog(
                    onDismiss = { viewModel.openCreateNestDialog(false) },
                    onCreate = { name -> viewModel.createNest(name) }
                )
            }

            if (uiState.isNoteEditorOpen) {
                DispatchNoteEditorDialog(
                    initialText = uiState.prefs.dispatchNoteText,
                    onDismiss = { viewModel.openNoteEditor(false) },
                    onSave = { text ->
                        viewModel.updateAppearance(dispatchNoteText = text)
                        viewModel.openNoteEditor(false)
                    }
                )
            }
        }
    }
}
