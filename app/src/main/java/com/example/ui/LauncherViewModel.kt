package com.example.ui

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.InstalledAppsManager
import com.example.data.LauncherPreferencesRepository
import com.example.model.AppInfo
import com.example.model.AvailableWidgetProvider
import com.example.model.DeviceStatus
import com.example.model.FolderSizeMode
import com.example.model.GestureActionType
import com.example.model.GestureBinding
import com.example.model.GestureId
import com.example.model.IconShapeStyle
import com.example.model.LauncherPreferencesState
import com.example.model.LayerTransitionStyle
import com.example.model.OwlAccentTheme
import com.example.model.OwlNativeWidgetType
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
import com.example.model.PlacedWidget
import com.example.model.PlacedWidgetKind
import com.example.model.StormMode
import com.example.model.WallpaperConfig
import com.example.model.WidgetSize
import com.example.widgets.OwlAppWidgetHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingWidgetBind(
    val appWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo
)

sealed interface ItemCustomizationTarget {
    data class AppTarget(val item: PlacedAppItem, val app: AppInfo) : ItemCustomizationTarget
    data class NestTarget(val nest: OwlNest) : ItemCustomizationTarget
    data class WidgetTarget(val widget: PlacedWidget) : ItemCustomizationTarget
}

data class LauncherUiState(
    val prefs: LauncherPreferencesState = LauncherPreferencesState(),
    val installedApps: List<AppInfo> = emptyList(),
    val isLoadingApps: Boolean = true,
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val isDefaultLauncher: Boolean = false,
    val availableWidgetProviders: List<AvailableWidgetProvider> = emptyList(),
    val currentPage: Int = 0,
    val isHomeControlBarOpen: Boolean = false,
    val isEditMode: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val initialSettingsTab: Int = 0,
    val isWidgetPickerOpen: Boolean = false,
    val settingsPreviewLandscape: Boolean? = null,
    val vaultUnlocked: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val selectedNestToOpen: OwlNest? = null,
    val appToAddToNest: AppInfo? = null,
    val itemCustomizationTarget: ItemCustomizationTarget? = null,
    val isCreateNestDialogOpen: Boolean = false,
    val isNoteEditorOpen: Boolean = false,
    val focusSecondsRemaining: Int = 25 * 60,
    val focusTimerRunning: Boolean = false,
    val wisdomIndex: Int = 0
) {
    private val appMap: Map<String, AppInfo> by lazy {
        installedApps.associateBy { it.componentKey }
    }

    fun findApp(key: String): AppInfo? = appMap[key]

    val visibleAppsInDrawer: List<AppInfo>
        get() = installedApps.filter { app ->
            val notHidden = app.componentKey !in prefs.hiddenAppKeys
            val focusAllowed = !prefs.focusPerchActive ||
                prefs.focusEssentialAppKeys.isEmpty() ||
                app.componentKey in prefs.focusEssentialAppKeys
            notHidden && focusAllowed
        }

    val vaultApps: List<AppInfo>
        get() = installedApps.filter { it.componentKey in prefs.hiddenAppKeys }

    fun placedAppsForPage(pageIndex: Int): List<Pair<PlacedAppItem, AppInfo>> {
        if (prefs.safetyShieldActive) return emptyList()
        return prefs.placedApps
            .filter { it.pageIndex.coerceIn(0, prefs.pageCount - 1) == pageIndex }
            .mapNotNull { placed ->
                val key = placed.componentKey
                if (key in prefs.hiddenAppKeys) return@mapNotNull null
                if (prefs.focusPerchActive && prefs.focusEssentialAppKeys.isNotEmpty() &&
                    key !in prefs.focusEssentialAppKeys
                ) {
                    return@mapNotNull null
                }
                val app = appMap[key] ?: return@mapNotNull null
                placed to app
            }
    }

    fun nestsForPage(pageIndex: Int): List<OwlNest> {
        if (prefs.safetyShieldActive) return emptyList()
        return prefs.owlNests.filter {
            it.pageIndex.coerceIn(0, prefs.pageCount - 1) == pageIndex
        }
    }

    fun widgetsForPage(pageIndex: Int): List<PlacedWidget> {
        if (prefs.safetyShieldActive) return emptyList()
        return prefs.placedWidgets.filter {
            it.pageIndex.coerceIn(0, prefs.pageCount - 1) == pageIndex
        }
    }

    val dockApps: List<AppInfo>
        get() = prefs.dockAppKeys.mapNotNull { key ->
            val app = appMap[key] ?: return@mapNotNull null
            if (key in prefs.hiddenAppKeys) return@mapNotNull null
            app
        }
}

sealed interface LauncherUiEvent {
    data class ShowMessage(val message: String) : LauncherUiEvent
    data class LaunchBindWidgetPermission(val intent: Intent) : LauncherUiEvent
    data class ConfigureAppWidget(val appWidgetId: Int) : LauncherUiEvent
    data class ScrollToPage(val pageIndex: Int) : LauncherUiEvent
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LauncherPreferencesRepository(application)
    val appsManager = InstalledAppsManager(application)
    val widgetHost = OwlAppWidgetHost(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    private val _isDefaultLauncher = MutableStateFlow(false)
    private val _availableProviders = MutableStateFlow<List<AvailableWidgetProvider>>(emptyList())
    private val _ephemeral = MutableStateFlow(EphemeralUiState())

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LauncherUiEvent>()
    val events: SharedFlow<LauncherUiEvent> = _events.asSharedFlow()

    private var packageReceiver: BroadcastReceiver? = null
    private var focusTimerJob: Job? = null
    private var pendingWidgetBind: PendingWidgetBind? = null
    private var torchEnabled: Boolean = false

    private data class EphemeralUiState(
        val currentPage: Int = 0,
        val isHomeControlBarOpen: Boolean = false,
        val isEditMode: Boolean = false,
        val isDrawerOpen: Boolean = false,
        val isSettingsOpen: Boolean = false,
        val initialSettingsTab: Int = 0,
        val isWidgetPickerOpen: Boolean = false,
        val settingsPreviewLandscape: Boolean? = null,
        val vaultUnlocked: Boolean = false,
        val selectedAppForMenu: AppInfo? = null,
        val selectedNestToOpen: OwlNest? = null,
        val appToAddToNest: AppInfo? = null,
        val itemCustomizationTarget: ItemCustomizationTarget? = null,
        val isCreateNestDialogOpen: Boolean = false,
        val isNoteEditorOpen: Boolean = false,
        val focusSecondsRemaining: Int = 25 * 60,
        val focusTimerRunning: Boolean = false,
        val wisdomIndex: Int = 0
    )

    init {
        viewModelScope.launch {
            combine(
                repository.preferencesFlow,
                _installedApps,
                _isLoadingApps,
                _deviceStatus,
                _isDefaultLauncher,
                _availableProviders,
                _ephemeral
            ) { values ->
                val prefs = values[0] as LauncherPreferencesState
                @Suppress("UNCHECKED_CAST")
                val apps = values[1] as List<AppInfo>
                val loading = values[2] as Boolean
                val device = values[3] as DeviceStatus
                val isDefault = values[4] as Boolean
                @Suppress("UNCHECKED_CAST")
                val providers = values[5] as List<AvailableWidgetProvider>
                val eph = values[6] as EphemeralUiState

                val clampedPage = eph.currentPage.coerceIn(0, (prefs.pageCount - 1).coerceAtLeast(0))

                LauncherUiState(
                    prefs = prefs,
                    installedApps = apps,
                    isLoadingApps = loading,
                    deviceStatus = device,
                    isDefaultLauncher = isDefault,
                    availableWidgetProviders = providers,
                    currentPage = clampedPage,
                    isHomeControlBarOpen = eph.isHomeControlBarOpen,
                    isEditMode = eph.isEditMode,
                    isDrawerOpen = eph.isDrawerOpen,
                    isSettingsOpen = eph.isSettingsOpen,
                    initialSettingsTab = eph.initialSettingsTab,
                    isWidgetPickerOpen = eph.isWidgetPickerOpen,
                    settingsPreviewLandscape = eph.settingsPreviewLandscape,
                    vaultUnlocked = eph.vaultUnlocked,
                    selectedAppForMenu = eph.selectedAppForMenu,
                    selectedNestToOpen = eph.selectedNestToOpen,
                    appToAddToNest = eph.appToAddToNest,
                    itemCustomizationTarget = eph.itemCustomizationTarget,
                    isCreateNestDialogOpen = eph.isCreateNestDialogOpen,
                    isNoteEditorOpen = eph.isNoteEditorOpen,
                    focusSecondsRemaining = eph.focusSecondsRemaining,
                    focusTimerRunning = eph.focusTimerRunning,
                    wisdomIndex = eph.wisdomIndex
                )
            }.collect { combined ->
                _uiState.value = combined
            }
        }

        packageReceiver = appsManager.registerPackageReceiver {
            refreshAppsAndProviders()
        }

        viewModelScope.launch {
            repository.ensureCleanHomeDefaultsOnce()
        }
        refreshAppsAndProviders()
        refreshSystemState()
    }

    fun refreshAppsAndProviders() {
        viewModelScope.launch {
            val apps = appsManager.loadInstalledApps()
            _installedApps.value = apps
            _isLoadingApps.value = false

            val dockInitialized = repository.dockInitializedFlow.first()
            if (!dockInitialized && apps.isNotEmpty()) {
                val starterDock = pickStarterDockApps(apps)
                repository.setDockApps(starterDock)
            }

            _availableProviders.value = widgetHost.loadAvailableProviders()
        }
    }

    private fun pickStarterDockApps(apps: List<AppInfo>): List<String> {
        val preferredKeywords = listOf(
            listOf("dialer", "phone", "telecom"),
            listOf("messaging", "mms", "sms", "messages"),
            listOf("browser", "chrome", "firefox"),
            listOf("camera"),
            listOf("settings")
        )
        val picked = mutableListOf<String>()
        for (group in preferredKeywords) {
            val match = apps.firstOrNull { app ->
                val lower = "${app.packageName} ${app.label}".lowercase()
                group.any { lower.contains(it) } && app.componentKey !in picked
            }
            if (match != null) picked.add(match.componentKey)
        }
        if (picked.size < 4) {
            apps.forEach { app ->
                if (picked.size < 4 && app.componentKey !in picked) {
                    picked.add(app.componentKey)
                }
            }
        }
        return picked.take(5)
    }

    fun refreshSystemState() {
        _isDefaultLauncher.value = appsManager.isDefaultLauncher()
        _deviceStatus.value = appsManager.readDeviceStatus(torchEnabled)
    }

    fun setCurrentPage(pageIndex: Int) {
        val maxPage = (_uiState.value.prefs.pageCount - 1).coerceAtLeast(0)
        _ephemeral.update { it.copy(currentPage = pageIndex.coerceIn(0, maxPage)) }
    }

    fun jumpToPage(pageIndex: Int) {
        val maxPage = (_uiState.value.prefs.pageCount - 1).coerceAtLeast(0)
        val target = pageIndex.coerceIn(0, maxPage)
        setCurrentPage(target)
        viewModelScope.launch {
            _events.emit(LauncherUiEvent.ScrollToPage(target))
        }
    }

    fun addHomePage() {
        viewModelScope.launch {
            val nextCount = (_uiState.value.prefs.pageCount + 1).coerceAtMost(8)
            repository.updateLayerConfig(pageCount = nextCount)
            jumpToPage(nextCount - 1)
        }
    }

    fun removeCurrentHomePage() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.prefs.pageCount <= 1) return@launch
            val removedIdx = state.currentPage
            val newCount = state.prefs.pageCount - 1

            // Shift items on pages after removedIdx down by 1, and move items on removedIdx to page 0
            val updatedApps = state.prefs.placedApps.map { item ->
                val p = when {
                    item.pageIndex == removedIdx -> 0
                    item.pageIndex > removedIdx -> item.pageIndex - 1
                    else -> item.pageIndex
                }
                item.copy(pageIndex = p.coerceIn(0, newCount - 1))
            }
            val updatedNests = state.prefs.owlNests.map { nest ->
                val p = when {
                    nest.pageIndex == removedIdx -> 0
                    nest.pageIndex > removedIdx -> nest.pageIndex - 1
                    else -> nest.pageIndex
                }
                nest.copy(pageIndex = p.coerceIn(0, newCount - 1))
            }
            val updatedWidgets = state.prefs.placedWidgets.map { w ->
                val p = when {
                    w.pageIndex == removedIdx -> 0
                    w.pageIndex > removedIdx -> w.pageIndex - 1
                    else -> w.pageIndex
                }
                w.copy(pageIndex = p.coerceIn(0, newCount - 1))
            }

            repository.setPlacedApps(updatedApps)
            repository.setOwlNests(updatedNests)
            repository.setPlacedWidgets(updatedWidgets)
            repository.updateLayerConfig(pageCount = newCount)
            jumpToPage((removedIdx - 1).coerceAtLeast(0))
        }
    }

    fun launchApp(app: AppInfo) {
        appsManager.launchApp(app)
        closeAllOverlays()
    }

    fun onHomeBackgroundLongPress() {
        _ephemeral.update {
            it.copy(
                isHomeControlBarOpen = true,
                isEditMode = true
            )
        }
    }

    fun closeHomeControlBarAndEdit() {
        _ephemeral.update {
            it.copy(
                isHomeControlBarOpen = false,
                isEditMode = false
            )
        }
    }

    fun openDrawer(open: Boolean) {
        _ephemeral.update {
            it.copy(
                isDrawerOpen = open,
                isHomeControlBarOpen = if (open) false else it.isHomeControlBarOpen,
                vaultUnlocked = if (!open) false else it.vaultUnlocked
            )
        }
    }

    fun openSettings(open: Boolean, initialTab: Int = 0) {
        if (open) refreshSystemState()
        _ephemeral.update {
            it.copy(
                isSettingsOpen = open,
                initialSettingsTab = initialTab,
                isHomeControlBarOpen = false,
                settingsPreviewLandscape = if (!open) null else it.settingsPreviewLandscape
            )
        }
    }

    fun setSettingsPreviewLandscape(landscape: Boolean?) {
        _ephemeral.update { it.copy(settingsPreviewLandscape = landscape) }
    }

    fun openWidgetPicker(open: Boolean) {
        if (open) {
            viewModelScope.launch {
                _availableProviders.value = widgetHost.loadAvailableProviders()
            }
        }
        _ephemeral.update {
            it.copy(
                isWidgetPickerOpen = open,
                isHomeControlBarOpen = false
            )
        }
    }

    fun setEditMode(enabled: Boolean) {
        _ephemeral.update {
            it.copy(
                isEditMode = enabled,
                isHomeControlBarOpen = enabled
            )
        }
    }

    fun closeAllOverlays() {
        _ephemeral.update {
            it.copy(
                isHomeControlBarOpen = false,
                isEditMode = false,
                isDrawerOpen = false,
                isSettingsOpen = false,
                isWidgetPickerOpen = false,
                settingsPreviewLandscape = null,
                selectedAppForMenu = null,
                selectedNestToOpen = null,
                appToAddToNest = null,
                itemCustomizationTarget = null,
                isCreateNestDialogOpen = false,
                isNoteEditorOpen = false,
                vaultUnlocked = false
            )
        }
    }

    fun handleBackPress(): Boolean {
        val eph = _ephemeral.value
        return when {
            eph.itemCustomizationTarget != null -> {
                _ephemeral.update { it.copy(itemCustomizationTarget = null) }
                true
            }
            eph.selectedAppForMenu != null -> {
                _ephemeral.update { it.copy(selectedAppForMenu = null) }
                true
            }
            eph.appToAddToNest != null -> {
                _ephemeral.update { it.copy(appToAddToNest = null) }
                true
            }
            eph.isCreateNestDialogOpen -> {
                _ephemeral.update { it.copy(isCreateNestDialogOpen = false) }
                true
            }
            eph.selectedNestToOpen != null -> {
                _ephemeral.update { it.copy(selectedNestToOpen = null) }
                true
            }
            eph.isNoteEditorOpen -> {
                _ephemeral.update { it.copy(isNoteEditorOpen = false) }
                true
            }
            eph.isWidgetPickerOpen -> {
                _ephemeral.update { it.copy(isWidgetPickerOpen = false) }
                true
            }
            eph.isSettingsOpen -> {
                openSettings(false)
                true
            }
            eph.isDrawerOpen -> {
                openDrawer(false)
                true
            }
            eph.isHomeControlBarOpen || eph.isEditMode -> {
                closeHomeControlBarAndEdit()
                true
            }
            else -> false
        }
    }

    fun showAppMenu(app: AppInfo?) {
        _ephemeral.update { it.copy(selectedAppForMenu = app) }
    }

    fun openItemCustomization(target: ItemCustomizationTarget?) {
        _ephemeral.update { it.copy(itemCustomizationTarget = target, selectedAppForMenu = null) }
    }

    fun openNest(nest: OwlNest?) {
        _ephemeral.update { it.copy(selectedNestToOpen = nest) }
    }

    fun openAddToNestPicker(app: AppInfo?) {
        _ephemeral.update { it.copy(appToAddToNest = app, selectedAppForMenu = null) }
    }

    fun openCreateNestDialog(open: Boolean) {
        _ephemeral.update { it.copy(isCreateNestDialogOpen = open, isHomeControlBarOpen = false) }
    }

    fun openNoteEditor(open: Boolean) {
        _ephemeral.update { it.copy(isNoteEditorOpen = open) }
    }

    fun setVaultUnlocked(unlocked: Boolean) {
        _ephemeral.update { it.copy(vaultUnlocked = unlocked) }
    }

    fun toggleNightOwlMode() {
        viewModelScope.launch {
            repository.updateAppearance(nightOwlMode = !_uiState.value.prefs.nightOwlMode)
        }
    }

    fun toggleFocusPerch() {
        viewModelScope.launch {
            repository.updateAppearance(focusPerchActive = !_uiState.value.prefs.focusPerchActive)
        }
    }

    fun toggleCleanCanvasMode() {
        viewModelScope.launch {
            repository.updateAppearance(cleanCanvasMode = !_uiState.value.prefs.cleanCanvasMode)
        }
    }

    fun toggleSafetyShield() {
        viewModelScope.launch {
            val next = !_uiState.value.prefs.safetyShieldActive
            repository.updateAppearance(safetyShieldActive = next)
            val msgRes = if (next) R.string.safety_shield_active_msg else R.string.safety_shield_off_msg
            emitMessage(getApplication<Application>().getString(msgRes))
        }
    }

    fun toggleFlashlight() {
        val next = !torchEnabled
        val ok = appsManager.setFlashlight(next)
        if (ok) {
            torchEnabled = next
            refreshSystemState()
        } else {
            emitMessage(getApplication<Application>().getString(R.string.torch_unavailable))
        }
    }

    fun toggleFocusTimer() {
        if (_ephemeral.value.focusTimerRunning) {
            focusTimerJob?.cancel()
            _ephemeral.update { it.copy(focusTimerRunning = false) }
        } else {
            _ephemeral.update { it.copy(focusTimerRunning = true) }
            focusTimerJob?.cancel()
            focusTimerJob = viewModelScope.launch {
                while (_ephemeral.value.focusSecondsRemaining > 0 && _ephemeral.value.focusTimerRunning) {
                    delay(1000L)
                    _ephemeral.update {
                        val next = (it.focusSecondsRemaining - 1).coerceAtLeast(0)
                        it.copy(
                            focusSecondsRemaining = next,
                            focusTimerRunning = next > 0
                        )
                    }
                }
            }
        }
    }

    fun resetFocusTimer() {
        focusTimerJob?.cancel()
        _ephemeral.update {
            it.copy(focusSecondsRemaining = 25 * 60, focusTimerRunning = false)
        }
    }

    fun cycleWisdomQuote() {
        _ephemeral.update { it.copy(wisdomIndex = (it.wisdomIndex + 1) % 5) }
    }

    // ---- Gestures Engine ----
    fun updateGestureBinding(gestureId: GestureId, binding: GestureBinding) {
        viewModelScope.launch {
            val updated = _uiState.value.prefs.gestureBindings.toMutableMap()
            updated[gestureId] = binding
            repository.setGestureBindings(updated)
        }
    }

    fun executeGesture(gestureId: GestureId) {
        val state = _uiState.value
        val binding = state.prefs.gestureBindings[gestureId] ?: return
        when (binding.action) {
            GestureActionType.NONE -> Unit
            GestureActionType.OPEN_DRAWER -> openDrawer(true)
            GestureActionType.TOGGLE_EDIT -> setEditMode(!state.isEditMode)
            GestureActionType.OPEN_WIDGETS -> openWidgetPicker(true)
            GestureActionType.NEXT_PAGE -> jumpToPage(state.currentPage + 1)
            GestureActionType.PREV_PAGE -> jumpToPage(state.currentPage - 1)
            GestureActionType.FIRST_PAGE -> jumpToPage(0)
            GestureActionType.LAST_PAGE -> jumpToPage(state.prefs.pageCount - 1)
            GestureActionType.OPEN_VAULT -> openDrawer(true)
            GestureActionType.TOGGLE_NIGHT_OWL -> toggleNightOwlMode()
            GestureActionType.TOGGLE_FOCUS -> toggleFocusPerch()
            GestureActionType.TOGGLE_CLEAN -> toggleCleanCanvasMode()
            GestureActionType.TOGGLE_SAFETY -> toggleSafetyShield()
            GestureActionType.TOGGLE_TORCH -> toggleFlashlight()
            GestureActionType.OPEN_NOTE -> openNoteEditor(true)
            GestureActionType.EXPAND_NOTIFICATIONS -> {
                appsManager.expandNotificationsShade()
            }
            GestureActionType.OPEN_DEVICE_SETTINGS -> openDeviceSettings(Settings.ACTION_SETTINGS)
            GestureActionType.LAUNCH_APP -> {
                val app = binding.targetAppKey?.let { state.findApp(it) }
                if (app != null) {
                    launchApp(app)
                }
            }
        }
    }

    // ---- Dropping, Moving & Resizing Apps on Home Layers ----
    fun togglePinHome(app: AppInfo) {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            val current = _uiState.value.prefs.placedApps.toMutableList()
            val existingIdx = current.indexOfFirst { it.componentKey == app.componentKey }
            val added = if (existingIdx >= 0) {
                current.removeAt(existingIdx)
                false
            } else {
                current.add(PlacedAppItem(componentKey = app.componentKey, pageIndex = page))
                true
            }
            repository.setPlacedApps(current)
            val msg = if (added) {
                getApplication<Application>().getString(R.string.msg_app_pinned)
            } else {
                getApplication<Application>().getString(R.string.msg_app_unpinned)
            }
            emitMessage(msg)
        }
    }

    fun dropAppToCurrentPage(app: AppInfo) {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            val current = _uiState.value.prefs.placedApps.toMutableList()
            current.removeAll { it.componentKey == app.componentKey }
            current.add(PlacedAppItem(componentKey = app.componentKey, pageIndex = page))
            repository.setPlacedApps(current)
            emitMessage(getApplication<Application>().getString(R.string.msg_app_pinned))
        }
    }

    fun updatePlacedAppItem(updated: PlacedAppItem) {
        viewModelScope.launch {
            val list = _uiState.value.prefs.placedApps.map {
                if (it.componentKey == updated.componentKey) updated else it
            }
            repository.setPlacedApps(list)
            val currentTarget = _ephemeral.value.itemCustomizationTarget
            if (currentTarget is ItemCustomizationTarget.AppTarget &&
                currentTarget.item.componentKey == updated.componentKey
            ) {
                _ephemeral.update {
                    it.copy(itemCustomizationTarget = currentTarget.copy(item = updated))
                }
            }
        }
    }

    fun swapPlacedApps(fromKey: String, toKey: String) {
        if (fromKey == toKey) return
        viewModelScope.launch {
            val list = _uiState.value.prefs.placedApps.toMutableList()
            val fromIdx = list.indexOfFirst { it.componentKey == fromKey }
            val toIdx = list.indexOfFirst { it.componentKey == toKey }
            if (fromIdx >= 0 && toIdx >= 0) {
                val tmp = list[fromIdx]
                list[fromIdx] = list[toIdx]
                list[toIdx] = tmp
                repository.setPlacedApps(list)
            }
        }
    }

    fun movePlacedAppStep(componentKey: String, delta: Int) {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            val all = _uiState.value.prefs.placedApps.toMutableList()
            val onPage = all.filter { it.pageIndex == page }.toMutableList()
            val idx = onPage.indexOfFirst { it.componentKey == componentKey }
            if (idx < 0) return@launch
            val targetIdx = (idx + delta).coerceIn(0, onPage.lastIndex)
            if (idx == targetIdx) return@launch
            val moved = onPage.removeAt(idx)
            onPage.add(targetIdx, moved)

            val otherPages = all.filterNot { it.pageIndex == page }
            repository.setPlacedApps(otherPages + onPage)
        }
    }

    fun movePlacedAppToPage(componentKey: String, targetPage: Int) {
        val clamped = targetPage.coerceIn(0, _uiState.value.prefs.pageCount - 1)
        viewModelScope.launch {
            val list = _uiState.value.prefs.placedApps.map {
                if (it.componentKey == componentKey) it.copy(pageIndex = clamped) else it
            }
            repository.setPlacedApps(list)
            jumpToPage(clamped)
        }
    }

    fun reorderOrMovePlacedApp(
        componentKey: String,
        targetPage: Int,
        targetIndexInPage: Int? = null
    ) {
        viewModelScope.launch {
            val maxPage = (_uiState.value.prefs.pageCount - 1).coerceAtLeast(0)
            val safePage = targetPage.coerceIn(0, maxPage)
            val all = _uiState.value.prefs.placedApps.toMutableList()
            val moving = all.firstOrNull { it.componentKey == componentKey }
                ?: PlacedAppItem(componentKey = componentKey, pageIndex = safePage)

            val withoutMoving = all.filterNot { it.componentKey == componentKey }
            val onTargetPage = withoutMoving.filter { it.pageIndex == safePage }.toMutableList()
            val otherPages = withoutMoving.filterNot { it.pageIndex == safePage }

            val insertIdx = (targetIndexInPage ?: onTargetPage.size).coerceIn(0, onTargetPage.size)
            onTargetPage.add(insertIdx, moving.copy(pageIndex = safePage))

            repository.setPlacedApps(otherPages + onTargetPage)
            if (_uiState.value.currentPage != safePage) {
                jumpToPage(safePage)
            }
        }
    }

    fun mergeAppsIntoFolder(draggedKey: String, targetKey: String, pageIndex: Int) {
        if (draggedKey == targetKey) return
        viewModelScope.launch {
            val state = _uiState.value
            val safePage = pageIndex.coerceIn(0, (state.prefs.pageCount - 1).coerceAtLeast(0))
            val updatedApps = state.prefs.placedApps.filterNot {
                it.componentKey == draggedKey || it.componentKey == targetKey
            }
            val nextNumber = state.prefs.owlNests.size + 1
            val autoName = getApplication<Application>().getString(
                R.string.auto_folder_name_format,
                nextNumber
            )
            val newNest = OwlNest(
                id = UUID.randomUUID().toString(),
                name = autoName,
                appKeys = listOf(targetKey, draggedKey),
                pageIndex = safePage
            )
            val updatedNests = state.prefs.owlNests + newNest
            repository.setPlacedApps(updatedApps)
            repository.setOwlNests(updatedNests)
        }
    }

    fun dropAppIntoNest(draggedKey: String, nestId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val updatedApps = state.prefs.placedApps.filterNot { it.componentKey == draggedKey }
            val updatedNests = state.prefs.owlNests.map { nest ->
                if (nest.id == nestId) {
                    nest.copy(appKeys = (nest.appKeys + draggedKey).distinct())
                } else {
                    nest
                }
            }
            repository.setPlacedApps(updatedApps)
            repository.setOwlNests(updatedNests)
        }
    }

    fun reorderOrMoveNest(nestId: String, targetPage: Int, targetIndexInPage: Int? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val maxPage = (state.prefs.pageCount - 1).coerceAtLeast(0)
            val safePage = targetPage.coerceIn(0, maxPage)
            val all = state.prefs.owlNests
            val moving = all.firstOrNull { it.id == nestId } ?: return@launch
            val withoutMoving = all.filterNot { it.id == nestId }
            val onTargetPage = withoutMoving.filter { it.pageIndex == safePage }.toMutableList()
            val otherPages = withoutMoving.filterNot { it.pageIndex == safePage }
            val insertIdx = (targetIndexInPage ?: onTargetPage.size).coerceIn(0, onTargetPage.size)
            onTargetPage.add(insertIdx, moving.copy(pageIndex = safePage))
            repository.setOwlNests(otherPages + onTargetPage)
        }
    }

    fun togglePinDock(app: AppInfo) {
        viewModelScope.launch {
            val current = _uiState.value.prefs.dockAppKeys.toMutableList()
            if (app.componentKey in current) {
                current.remove(app.componentKey)
                repository.setDockApps(current)
            } else if (current.size >= 5) {
                emitMessage(getApplication<Application>().getString(R.string.msg_dock_full))
            } else {
                current.add(app.componentKey)
                repository.setDockApps(current)
            }
        }
    }

    fun toggleHideInVault(app: AppInfo) {
        viewModelScope.launch {
            val hidden = _uiState.value.prefs.hiddenAppKeys.toMutableSet()
            if (app.componentKey in hidden) {
                hidden.remove(app.componentKey)
            } else {
                hidden.add(app.componentKey)
                val pinned = _uiState.value.prefs.placedApps.filterNot { it.componentKey == app.componentKey }
                val dock = _uiState.value.prefs.dockAppKeys.filterNot { it == app.componentKey }
                repository.setPlacedApps(pinned)
                repository.setDockApps(dock)
            }
            repository.setHiddenApps(hidden)
        }
    }

    fun toggleFocusEssential(app: AppInfo) {
        viewModelScope.launch {
            val essentials = _uiState.value.prefs.focusEssentialAppKeys.toMutableSet()
            if (app.componentKey in essentials) {
                essentials.remove(app.componentKey)
            } else {
                essentials.add(app.componentKey)
            }
            repository.setFocusEssentialApps(essentials)
        }
    }

    // ---- Owl Nests (Folders) + Size/Shape/GIF ----
    fun createNest(name: String, initialApp: AppInfo? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val nests = _uiState.value.prefs.owlNests.toMutableList()
            val newNest = OwlNest(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                appKeys = listOfNotNull(initialApp?.componentKey),
                pageIndex = _uiState.value.currentPage
            )
            nests.add(newNest)
            repository.setOwlNests(nests)
            _ephemeral.update { it.copy(isCreateNestDialogOpen = false, appToAddToNest = null) }
        }
    }

    fun addAppToNest(nestId: String, app: AppInfo) {
        viewModelScope.launch {
            val nests = _uiState.value.prefs.owlNests.map { nest ->
                if (nest.id == nestId) {
                    val updatedKeys = (nest.appKeys + app.componentKey).distinct()
                    nest.copy(appKeys = updatedKeys)
                } else {
                    nest
                }
            }
            repository.setOwlNests(nests)
            _ephemeral.update { it.copy(appToAddToNest = null) }
        }
    }

    fun removeAppFromNest(nestId: String, appKey: String) {
        viewModelScope.launch {
            val currentNests = _uiState.value.prefs.owlNests
            val target = currentNests.firstOrNull { it.id == nestId } ?: return@launch
            val remaining = target.appKeys.filterNot { it == appKey }
            if (remaining.isEmpty()) {
                val nests = currentNests.filterNot { it.id == nestId }
                repository.setOwlNests(nests)
                if (_ephemeral.value.selectedNestToOpen?.id == nestId) {
                    _ephemeral.update { it.copy(selectedNestToOpen = null) }
                }
            } else {
                val updated = target.copy(appKeys = remaining)
                val nests = currentNests.map { if (it.id == nestId) updated else it }
                repository.setOwlNests(nests)
                if (_ephemeral.value.selectedNestToOpen?.id == nestId) {
                    _ephemeral.update { it.copy(selectedNestToOpen = updated) }
                }
            }
        }
    }

    fun reorderAppInNest(nestId: String, appKey: String, targetIndex: Int) {
        viewModelScope.launch {
            val currentNests = _uiState.value.prefs.owlNests
            val target = currentNests.firstOrNull { it.id == nestId } ?: return@launch
            val mutable = target.appKeys.toMutableList()
            val oldIdx = mutable.indexOf(appKey)
            if (oldIdx < 0) return@launch
            val moved = mutable.removeAt(oldIdx)
            val safeTarget = targetIndex.coerceIn(0, mutable.size)
            mutable.add(safeTarget, moved)
            val updated = target.copy(appKeys = mutable)
            val nests = currentNests.map { if (it.id == nestId) updated else it }
            repository.setOwlNests(nests)
            if (_ephemeral.value.selectedNestToOpen?.id == nestId) {
                _ephemeral.update { it.copy(selectedNestToOpen = updated) }
            }
        }
    }

    fun extractAppFromNestToHome(nestId: String, appKey: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentNests = state.prefs.owlNests
            val target = currentNests.firstOrNull { it.id == nestId } ?: return@launch
            val remaining = target.appKeys.filterNot { it == appKey }
            val updatedNests = if (remaining.isEmpty()) {
                currentNests.filterNot { it.id == nestId }
            } else {
                currentNests.map {
                    if (it.id == nestId) it.copy(appKeys = remaining) else it
                }
            }

            val page = state.currentPage.coerceIn(0, (state.prefs.pageCount - 1).coerceAtLeast(0))
            val currentApps = state.prefs.placedApps.filterNot { it.componentKey == appKey }
            val updatedApps = currentApps + PlacedAppItem(componentKey = appKey, pageIndex = page)

            repository.setOwlNests(updatedNests)
            repository.setPlacedApps(updatedApps)
            _ephemeral.update { it.copy(selectedNestToOpen = null) }
        }
    }

    fun updateNest(updated: OwlNest) {
        viewModelScope.launch {
            val nests = _uiState.value.prefs.owlNests.map {
                if (it.id == updated.id) updated else it
            }
            repository.setOwlNests(nests)
            val currentTarget = _ephemeral.value.itemCustomizationTarget
            if (currentTarget is ItemCustomizationTarget.NestTarget &&
                currentTarget.nest.id == updated.id
            ) {
                _ephemeral.update {
                    it.copy(itemCustomizationTarget = ItemCustomizationTarget.NestTarget(updated))
                }
            }
            if (_ephemeral.value.selectedNestToOpen?.id == updated.id) {
                _ephemeral.update { it.copy(selectedNestToOpen = updated) }
            }
        }
    }

    fun attachGifToNest(nestId: String, uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.saveMediaCopyFromUri(uri, "nest_$nestId") ?: return@launch
            val target = _uiState.value.prefs.owlNests.firstOrNull { it.id == nestId } ?: return@launch
            updateNest(target.copy(gifPath = savedPath))
        }
    }

    fun swapNests(fromId: String, toId: String) {
        if (fromId == toId) return
        viewModelScope.launch {
            val list = _uiState.value.prefs.owlNests.toMutableList()
            val fromIdx = list.indexOfFirst { it.id == fromId }
            val toIdx = list.indexOfFirst { it.id == toId }
            if (fromIdx >= 0 && toIdx >= 0) {
                val tmp = list[fromIdx]
                list[fromIdx] = list[toIdx]
                list[toIdx] = tmp
                repository.setOwlNests(list)
            }
        }
    }

    fun deleteNest(nestId: String) {
        viewModelScope.launch {
            val nests = _uiState.value.prefs.owlNests.filterNot { it.id == nestId }
            repository.setOwlNests(nests)
            _ephemeral.update {
                it.copy(
                    selectedNestToOpen = null,
                    itemCustomizationTarget = null
                )
            }
        }
    }

    // ---- Wallpapers, Storm Engine, Layers & Style ----
    fun saveWallpaperFromUri(isLandscape: Boolean, uri: Uri) {
        viewModelScope.launch {
            if (isLandscape) {
                repository.saveLandscapeWallpaperFromUri(uri)
            } else {
                repository.savePortraitWallpaperFromUri(uri)
            }
        }
    }

    fun resetWallpaper(isLandscape: Boolean) {
        viewModelScope.launch {
            repository.resetWallpaper(isLandscape)
        }
    }

    fun updateWallpaperConfig(isLandscape: Boolean, config: WallpaperConfig) {
        viewModelScope.launch {
            repository.updateWallpaperConfig(isLandscape, config)
        }
    }

    fun updateStormConfig(
        stormMode: StormMode? = null,
        rainDensity: Float? = null,
        lightningEnabled: Boolean? = null,
        stormFogEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateStormConfig(stormMode, rainDensity, lightningEnabled, stormFogEnabled)
        }
    }

    fun updateLayerConfig(
        layerTransition: LayerTransitionStyle? = null,
        showDock: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateLayerConfig(layerTransition = layerTransition, showDock = showDock)
        }
    }

    fun updateIconStyle(
        shape: IconShapeStyle? = null,
        sizeDp: Int? = null,
        frameBackdrop: Boolean? = null,
        showLabels: Boolean? = null,
        portraitColumns: Int? = null,
        landscapeColumns: Int? = null
    ) {
        viewModelScope.launch {
            repository.updateIconStyle(
                shape = shape,
                sizeDp = sizeDp,
                frameBackdrop = frameBackdrop,
                showLabels = showLabels,
                portraitColumns = portraitColumns,
                landscapeColumns = landscapeColumns
            )
        }
    }

    fun updateAppearance(
        accentTheme: OwlAccentTheme? = null,
        glassOpacity: Float? = null,
        requireVaultLock: Boolean? = null,
        editorialCustomTitle: String? = null,
        dispatchNoteText: String? = null
    ) {
        viewModelScope.launch {
            repository.updateAppearance(
                accentTheme = accentTheme,
                glassOpacity = glassOpacity,
                requireVaultLock = requireVaultLock,
                editorialCustomTitle = editorialCustomTitle,
                dispatchNoteText = dispatchNoteText
            )
        }
    }

    // ---- Widgets & GIF Stickers ----
    fun addOwlNativeWidget(type: OwlNativeWidgetType) {
        viewModelScope.launch {
            val current = _uiState.value.prefs.placedWidgets.toMutableList()
            current.add(
                PlacedWidget(
                    id = "owl_${type.name}_${System.currentTimeMillis()}",
                    kind = PlacedWidgetKind.OWL_NATIVE,
                    pageIndex = _uiState.value.currentPage,
                    nativeType = type,
                    size = WidgetSize.WIDE
                )
            )
            repository.setPlacedWidgets(current)
            _ephemeral.update { it.copy(isWidgetPickerOpen = false) }
            emitMessage(getApplication<Application>().getString(R.string.widget_added))
        }
    }

    fun requestAddSystemWidget(provider: AvailableWidgetProvider) {
        val providerInfo = widgetHost.findProviderInfo(
            provider.providerPackage,
            provider.providerClass
        ) ?: return

        val appWidgetId = widgetHost.allocateWidgetId()
        if (widgetHost.tryBindWidget(appWidgetId, providerInfo)) {
            completeSystemWidgetAdd(appWidgetId, providerInfo)
        } else {
            pendingWidgetBind = PendingWidgetBind(appWidgetId, providerInfo)
            val bindIntent = widgetHost.createBindPermissionIntent(appWidgetId, providerInfo)
            viewModelScope.launch {
                _events.emit(LauncherUiEvent.LaunchBindWidgetPermission(bindIntent))
            }
        }
    }

    fun onBindWidgetResult(resultCode: Int) {
        val pending = pendingWidgetBind ?: return
        pendingWidgetBind = null
        if (resultCode == Activity.RESULT_OK) {
            completeSystemWidgetAdd(pending.appWidgetId, pending.providerInfo)
        } else {
            widgetHost.deleteWidgetId(pending.appWidgetId)
            emitMessage(getApplication<Application>().getString(R.string.widget_bind_denied))
        }
    }

    private fun completeSystemWidgetAdd(appWidgetId: Int, providerInfo: AppWidgetProviderInfo) {
        if (widgetHost.needsConfiguration(providerInfo)) {
            pendingWidgetBind = PendingWidgetBind(appWidgetId, providerInfo)
            viewModelScope.launch {
                _events.emit(LauncherUiEvent.ConfigureAppWidget(appWidgetId))
            }
        } else {
            finalizePlacedSystemWidget(appWidgetId, providerInfo)
        }
    }

    fun onConfigureWidgetResult(resultCode: Int) {
        val pending = pendingWidgetBind ?: return
        pendingWidgetBind = null
        if (resultCode == Activity.RESULT_OK) {
            finalizePlacedSystemWidget(pending.appWidgetId, pending.providerInfo)
        } else {
            widgetHost.deleteWidgetId(pending.appWidgetId)
        }
    }

    private fun finalizePlacedSystemWidget(appWidgetId: Int, providerInfo: AppWidgetProviderInfo) {
        viewModelScope.launch {
            val current = _uiState.value.prefs.placedWidgets.toMutableList()
            current.add(
                PlacedWidget(
                    id = "sys_$appWidgetId",
                    kind = PlacedWidgetKind.ANDROID_APPWIDGET,
                    pageIndex = _uiState.value.currentPage,
                    appWidgetId = appWidgetId,
                    providerPackage = providerInfo.provider.packageName,
                    providerClass = providerInfo.provider.className,
                    size = WidgetSize.WIDE
                )
            )
            repository.setPlacedWidgets(current)
            _ephemeral.update { it.copy(isWidgetPickerOpen = false) }
            emitMessage(getApplication<Application>().getString(R.string.widget_added))
        }
    }

    fun updateWidget(updated: PlacedWidget) {
        viewModelScope.launch {
            val list = _uiState.value.prefs.placedWidgets.map {
                if (it.id == updated.id) updated else it
            }
            repository.setPlacedWidgets(list)
            val currentTarget = _ephemeral.value.itemCustomizationTarget
            if (currentTarget is ItemCustomizationTarget.WidgetTarget &&
                currentTarget.widget.id == updated.id
            ) {
                _ephemeral.update {
                    it.copy(itemCustomizationTarget = ItemCustomizationTarget.WidgetTarget(updated))
                }
            }
        }
    }

    fun attachGifToWidget(widgetId: String, uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.saveMediaCopyFromUri(uri, "widget_$widgetId") ?: return@launch
            val target = _uiState.value.prefs.placedWidgets.firstOrNull { it.id == widgetId } ?: return@launch
            updateWidget(target.copy(gifPath = savedPath))
        }
    }

    fun cycleWidgetSize(widgetId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.prefs.placedWidgets.map { widget ->
                if (widget.id == widgetId) {
                    val nextSize = when (widget.size) {
                        WidgetSize.COMPACT -> WidgetSize.WIDE
                        WidgetSize.WIDE -> WidgetSize.TALL
                        WidgetSize.TALL -> WidgetSize.COMPACT
                    }
                    widget.copy(size = nextSize, customHeightDp = null)
                } else {
                    widget
                }
            }
            repository.setPlacedWidgets(updated)
        }
    }

    fun moveWidget(widgetId: String, direction: Int) {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            val all = _uiState.value.prefs.placedWidgets.toMutableList()
            val onPage = all.filter { it.pageIndex == page }.toMutableList()
            val idx = onPage.indexOfFirst { it.id == widgetId }
            if (idx < 0) return@launch
            val targetIdx = (idx + direction).coerceIn(0, onPage.lastIndex)
            if (idx != targetIdx) {
                val item = onPage.removeAt(idx)
                onPage.add(targetIdx, item)
                val otherPages = all.filterNot { it.pageIndex == page }
                repository.setPlacedWidgets(otherPages + onPage)
            }
        }
    }

    fun removeWidget(widgetId: String) {
        viewModelScope.launch {
            val list = _uiState.value.prefs.placedWidgets.toMutableList()
            val target = list.firstOrNull { it.id == widgetId } ?: return@launch
            if (target.kind == PlacedWidgetKind.ANDROID_APPWIDGET && target.appWidgetId > 0) {
                widgetHost.deleteWidgetId(target.appWidgetId)
            }
            list.removeAll { it.id == widgetId }
            repository.setPlacedWidgets(list)
            _ephemeral.update { it.copy(itemCustomizationTarget = null) }
        }
    }

    fun openDeviceSettings(action: String = Settings.ACTION_SETTINGS) {
        appsManager.openSystemSettings(action)
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _events.emit(LauncherUiEvent.ShowMessage(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        packageReceiver?.let {
            try {
                getApplication<Application>().unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
    }
}
