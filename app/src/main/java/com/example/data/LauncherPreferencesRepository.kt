package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FolderSizeMode
import com.example.model.GestureActionType
import com.example.model.GestureBinding
import com.example.model.GestureId
import com.example.model.HomeStickyText
import com.example.model.IconShapeStyle
import com.example.model.LauncherPreferencesState
import com.example.model.LayerTransitionStyle
import com.example.model.OwlAccentTheme
import com.example.model.OwlNativeWidgetType
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
import com.example.model.PlacedWidget
import com.example.model.PlacedWidgetKind
import com.example.model.RecentAppEntry
import com.example.model.StormMode
import com.example.model.WallpaperConfig
import com.example.model.WallpaperScaleMode
import com.example.model.WidgetSize
import com.example.model.defaultGestureBindings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "owl_launcher_prefs"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val PORTRAIT_WALLPAPER_PATH = stringPreferencesKey("portrait_wallpaper_path")
        val PORTRAIT_WALLPAPER_VERSION = longPreferencesKey("portrait_wallpaper_version")
        val PORTRAIT_SCALE_MODE = stringPreferencesKey("portrait_scale_mode")
        val PORTRAIT_ZOOM = floatPreferencesKey("portrait_zoom")
        val PORTRAIT_OFFSET_X = floatPreferencesKey("portrait_offset_x")
        val PORTRAIT_OFFSET_Y = floatPreferencesKey("portrait_offset_y")
        val PORTRAIT_DIM = floatPreferencesKey("portrait_dim")

        val LANDSCAPE_WALLPAPER_PATH = stringPreferencesKey("landscape_wallpaper_path")
        val LANDSCAPE_WALLPAPER_VERSION = longPreferencesKey("landscape_wallpaper_version")
        val LANDSCAPE_SCALE_MODE = stringPreferencesKey("landscape_scale_mode")
        val LANDSCAPE_ZOOM = floatPreferencesKey("landscape_zoom")
        val LANDSCAPE_OFFSET_X = floatPreferencesKey("landscape_offset_x")
        val LANDSCAPE_OFFSET_Y = floatPreferencesKey("landscape_offset_y")
        val LANDSCAPE_DIM = floatPreferencesKey("landscape_dim")

        val STORM_MODE = stringPreferencesKey("storm_mode")
        val RAIN_DENSITY = floatPreferencesKey("rain_density")
        val LIGHTNING_ENABLED = booleanPreferencesKey("lightning_enabled")
        val STORM_FOG_ENABLED = booleanPreferencesKey("storm_fog_enabled")
        val LAYER_TRANSITION = stringPreferencesKey("layer_transition")
        val PAGE_COUNT = intPreferencesKey("page_count")
        val SHOW_DOCK = booleanPreferencesKey("show_dock")
        val SHOW_RECENTS_IN_DOCK = booleanPreferencesKey("show_recents_in_dock")
        val SYNC_SYSTEM_WALLPAPER = booleanPreferencesKey("sync_system_wallpaper")
        val LAST_SYNCED_WALLPAPER_SIG = stringPreferencesKey("last_synced_wallpaper_sig")

        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val ICON_SIZE_DP = intPreferencesKey("icon_size_dp")
        val ICON_FRAME_BACKDROP = booleanPreferencesKey("icon_frame_backdrop")
        val SHOW_APP_LABELS = booleanPreferencesKey("show_app_labels")
        val PORTRAIT_COLUMNS = intPreferencesKey("portrait_columns")
        val LANDSCAPE_COLUMNS = intPreferencesKey("landscape_columns")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")

        val NIGHT_OWL_MODE = booleanPreferencesKey("night_owl_mode")
        val FOCUS_PERCH_ACTIVE = booleanPreferencesKey("focus_perch_active")
        val CLEAN_CANVAS_MODE = booleanPreferencesKey("clean_canvas_mode")
        val SAFETY_SHIELD_ACTIVE = booleanPreferencesKey("safety_shield_active")
        val REQUIRE_VAULT_LOCK = booleanPreferencesKey("require_vault_lock")
        val EDITORIAL_TITLE = stringPreferencesKey("editorial_title")
        val DISPATCH_NOTE = stringPreferencesKey("dispatch_note")

        val PLACED_APPS_JSON = stringPreferencesKey("placed_apps_v2_json")
        val LEGACY_PINNED_HOME_APPS_JSON = stringPreferencesKey("pinned_home_apps_json")
        val DOCK_APPS_JSON = stringPreferencesKey("dock_apps_json")
        val DOCK_INITIALIZED = booleanPreferencesKey("dock_initialized")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val FOCUS_ESSENTIAL_APPS = stringSetPreferencesKey("focus_essential_apps")
        val OWL_NESTS_JSON = stringPreferencesKey("owl_nests_json")
        val PLACED_WIDGETS_JSON = stringPreferencesKey("placed_widgets_json")
        val STICKY_TEXTS_JSON = stringPreferencesKey("sticky_texts_v1_json")
        val WIDGETS_INITIALIZED = booleanPreferencesKey("widgets_initialized")
        val RECENT_APPS_JSON = stringPreferencesKey("recent_apps_v1_json")
        val GESTURES_JSON = stringPreferencesKey("gestures_v1_json")
        val CLEAN_HOME_V2_MIGRATED = booleanPreferencesKey("clean_home_v2_migrated")
        val TYPER_GESTURE_V1_MIGRATED = booleanPreferencesKey("typer_gesture_v1_migrated")
    }

    val preferencesFlow: Flow<LauncherPreferencesState> = context.launcherDataStore.data.map { prefs ->
        val defaultState = LauncherPreferencesState()

        val portraitWallpaper = WallpaperConfig(
            customFilePath = prefs[Keys.PORTRAIT_WALLPAPER_PATH]?.takeIf { File(it).exists() },
            version = prefs[Keys.PORTRAIT_WALLPAPER_VERSION] ?: 0L,
            scaleMode = prefs[Keys.PORTRAIT_SCALE_MODE].toEnumOrDefault(WallpaperScaleMode.CROP),
            zoom = (prefs[Keys.PORTRAIT_ZOOM] ?: 1.0f).coerceIn(1.0f, 2.5f),
            offsetX = (prefs[Keys.PORTRAIT_OFFSET_X] ?: 0f).coerceIn(-1f, 1f),
            offsetY = (prefs[Keys.PORTRAIT_OFFSET_Y] ?: 0f).coerceIn(-1f, 1f),
            dimAlpha = (prefs[Keys.PORTRAIT_DIM] ?: 0.14f).coerceIn(0f, 0.75f)
        )

        val landscapeWallpaper = WallpaperConfig(
            customFilePath = prefs[Keys.LANDSCAPE_WALLPAPER_PATH]?.takeIf { File(it).exists() },
            version = prefs[Keys.LANDSCAPE_WALLPAPER_VERSION] ?: 0L,
            scaleMode = prefs[Keys.LANDSCAPE_SCALE_MODE].toEnumOrDefault(WallpaperScaleMode.CROP),
            zoom = (prefs[Keys.LANDSCAPE_ZOOM] ?: 1.0f).coerceIn(1.0f, 2.5f),
            offsetX = (prefs[Keys.LANDSCAPE_OFFSET_X] ?: 0f).coerceIn(-1f, 1f),
            offsetY = (prefs[Keys.LANDSCAPE_OFFSET_Y] ?: 0f).coerceIn(-1f, 1f),
            dimAlpha = (prefs[Keys.LANDSCAPE_DIM] ?: 0.16f).coerceIn(0f, 0.75f)
        )

        val widgetsInitialized = prefs[Keys.WIDGETS_INITIALIZED] ?: false
        val placedWidgets = if (widgetsInitialized) {
            decodeWidgets(prefs[Keys.PLACED_WIDGETS_JSON])
        } else {
            defaultState.placedWidgets
        }

        val placedApps = prefs[Keys.PLACED_APPS_JSON]?.let { decodePlacedApps(it) }
            ?: decodeStringList(prefs[Keys.LEGACY_PINNED_HOME_APPS_JSON]).map {
                PlacedAppItem(componentKey = it, pageIndex = 0)
            }

        LauncherPreferencesState(
            portraitWallpaper = portraitWallpaper,
            landscapeWallpaper = landscapeWallpaper,
            stormMode = prefs[Keys.STORM_MODE].toEnumOrDefault(StormMode.THUNDER_STORM),
            rainDensity = (prefs[Keys.RAIN_DENSITY] ?: 0.65f).coerceIn(0.1f, 1.0f),
            lightningEnabled = prefs[Keys.LIGHTNING_ENABLED] ?: true,
            stormFogEnabled = prefs[Keys.STORM_FOG_ENABLED] ?: true,
            layerTransition = prefs[Keys.LAYER_TRANSITION].toEnumOrDefault(LayerTransitionStyle.DEPTH_PARALLAX),
            pageCount = (prefs[Keys.PAGE_COUNT] ?: 2).coerceIn(1, 8),
            showDock = prefs[Keys.SHOW_DOCK] ?: true,
            showRecentsInDock = prefs[Keys.SHOW_RECENTS_IN_DOCK] ?: false,
            syncSystemWallpaper = prefs[Keys.SYNC_SYSTEM_WALLPAPER] ?: true,
            iconShape = prefs[Keys.ICON_SHAPE].toEnumOrDefault(IconShapeStyle.ORIGINAL),
            iconSizeDp = (prefs[Keys.ICON_SIZE_DP] ?: 54).coerceIn(40, 72),
            showIconFrameBackdrop = prefs[Keys.ICON_FRAME_BACKDROP] ?: false,
            showAppLabels = prefs[Keys.SHOW_APP_LABELS] ?: false,
            portraitColumns = (prefs[Keys.PORTRAIT_COLUMNS] ?: 4).coerceIn(3, 6),
            landscapeColumns = (prefs[Keys.LANDSCAPE_COLUMNS] ?: 5).coerceIn(3, 7),
            glassOpacity = (prefs[Keys.GLASS_OPACITY] ?: 0.54f).coerceIn(0.15f, 0.90f),
            accentTheme = prefs[Keys.ACCENT_THEME].toEnumOrDefault(OwlAccentTheme.AMBER),
            nightOwlMode = prefs[Keys.NIGHT_OWL_MODE] ?: false,
            focusPerchActive = prefs[Keys.FOCUS_PERCH_ACTIVE] ?: false,
            cleanCanvasMode = prefs[Keys.CLEAN_CANVAS_MODE] ?: false,
            safetyShieldActive = prefs[Keys.SAFETY_SHIELD_ACTIVE] ?: false,
            requireVaultDeviceLock = prefs[Keys.REQUIRE_VAULT_LOCK] ?: true,
            editorialCustomTitle = prefs[Keys.EDITORIAL_TITLE] ?: "",
            dispatchNoteText = prefs[Keys.DISPATCH_NOTE] ?: "",
            placedApps = placedApps,
            dockAppKeys = decodeStringList(prefs[Keys.DOCK_APPS_JSON]),
            hiddenAppKeys = prefs[Keys.HIDDEN_APPS] ?: emptySet(),
            focusEssentialAppKeys = prefs[Keys.FOCUS_ESSENTIAL_APPS] ?: emptySet(),
            owlNests = decodeNests(prefs[Keys.OWL_NESTS_JSON]),
            placedWidgets = placedWidgets,
            stickyTexts = decodeStickyTexts(prefs[Keys.STICKY_TEXTS_JSON]),
            recentApps = decodeRecentApps(prefs[Keys.RECENT_APPS_JSON]),
            gestureBindings = decodeGestures(prefs[Keys.GESTURES_JSON])
        )
    }

    val dockInitializedFlow: Flow<Boolean> = context.launcherDataStore.data.map { prefs ->
        prefs[Keys.DOCK_INITIALIZED] ?: false
    }

    val lastSyncedWallpaperSignatureFlow: Flow<String?> = context.launcherDataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNCED_WALLPAPER_SIG]
    }

    suspend fun setSyncSystemWallpaper(enabled: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.SYNC_SYSTEM_WALLPAPER] = enabled
        }
    }

    suspend fun setLastSyncedWallpaperSignature(signature: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_WALLPAPER_SIG] = signature
        }
    }

    suspend fun ensureCleanHomeDefaultsOnce() {
        context.launcherDataStore.edit { prefs ->
            if (prefs[Keys.CLEAN_HOME_V2_MIGRATED] != true) {
                prefs[Keys.SHOW_APP_LABELS] = false
                val existingWidgets = decodeWidgets(prefs[Keys.PLACED_WIDGETS_JSON])
                val filteredWidgets = existingWidgets.filterNot {
                    it.id == "owl_native_system_default" || it.id == "owl_native_wisdom_default"
                }
                if (existingWidgets.size != filteredWidgets.size) {
                    prefs[Keys.PLACED_WIDGETS_JSON] = encodeWidgets(filteredWidgets)
                }
                prefs[Keys.CLEAN_HOME_V2_MIGRATED] = true
            }
            if (prefs[Keys.TYPER_GESTURE_V1_MIGRATED] != true) {
                val currentGestures = decodeGestures(prefs[Keys.GESTURES_JSON]).toMutableMap()
                currentGestures[GestureId.FOUR_F_TAP] = GestureBinding(GestureActionType.OPEN_HOME_TYPER)
                currentGestures[GestureId.EIGHT_F_TAP] = GestureBinding(GestureActionType.OPEN_HOME_TYPER)
                prefs[Keys.GESTURES_JSON] = encodeGestures(currentGestures)
                prefs[Keys.TYPER_GESTURE_V1_MIGRATED] = true
            }
        }
    }

    suspend fun savePortraitWallpaperFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val dest = File(context.filesDir, "owl_wallpaper_portrait.jpg")
        val copied = copyUriToFile(uri, dest)
        if (copied) {
            context.launcherDataStore.edit { prefs ->
                prefs[Keys.PORTRAIT_WALLPAPER_PATH] = dest.absolutePath
                prefs[Keys.PORTRAIT_WALLPAPER_VERSION] = System.currentTimeMillis()
            }
        }
        copied
    }

    suspend fun saveLandscapeWallpaperFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val dest = File(context.filesDir, "owl_wallpaper_landscape.jpg")
        val copied = copyUriToFile(uri, dest)
        if (copied) {
            context.launcherDataStore.edit { prefs ->
                prefs[Keys.LANDSCAPE_WALLPAPER_PATH] = dest.absolutePath
                prefs[Keys.LANDSCAPE_WALLPAPER_VERSION] = System.currentTimeMillis()
            }
        }
        copied
    }

    suspend fun saveMediaCopyFromUri(uri: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "owl_media").apply { mkdirs() }
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("gif", ignoreCase = true) -> "gif"
            mime.contains("webp", ignoreCase = true) -> "webp"
            mime.contains("png", ignoreCase = true) -> "png"
            else -> "gif"
        }
        val dest = File(dir, "${prefix}_${System.currentTimeMillis()}.$ext")
        if (copyUriToFile(uri, dest)) dest.absolutePath else null
    }

    private fun copyUriToFile(uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun resetWallpaper(isLandscape: Boolean) {
        context.launcherDataStore.edit { prefs ->
            if (isLandscape) {
                prefs.remove(Keys.LANDSCAPE_WALLPAPER_PATH)
                prefs[Keys.LANDSCAPE_WALLPAPER_VERSION] = System.currentTimeMillis()
                File(context.filesDir, "owl_wallpaper_landscape.jpg").delete()
            } else {
                prefs.remove(Keys.PORTRAIT_WALLPAPER_PATH)
                prefs[Keys.PORTRAIT_WALLPAPER_VERSION] = System.currentTimeMillis()
                File(context.filesDir, "owl_wallpaper_portrait.jpg").delete()
            }
        }
    }

    suspend fun updateWallpaperConfig(isLandscape: Boolean, config: WallpaperConfig) {
        context.launcherDataStore.edit { prefs ->
            if (isLandscape) {
                prefs[Keys.LANDSCAPE_SCALE_MODE] = config.scaleMode.name
                prefs[Keys.LANDSCAPE_ZOOM] = config.zoom
                prefs[Keys.LANDSCAPE_OFFSET_X] = config.offsetX
                prefs[Keys.LANDSCAPE_OFFSET_Y] = config.offsetY
                prefs[Keys.LANDSCAPE_DIM] = config.dimAlpha
            } else {
                prefs[Keys.PORTRAIT_SCALE_MODE] = config.scaleMode.name
                prefs[Keys.PORTRAIT_ZOOM] = config.zoom
                prefs[Keys.PORTRAIT_OFFSET_X] = config.offsetX
                prefs[Keys.PORTRAIT_OFFSET_Y] = config.offsetY
                prefs[Keys.PORTRAIT_DIM] = config.dimAlpha
            }
        }
    }

    suspend fun updateStormConfig(
        stormMode: StormMode? = null,
        rainDensity: Float? = null,
        lightningEnabled: Boolean? = null,
        stormFogEnabled: Boolean? = null
    ) {
        context.launcherDataStore.edit { prefs ->
            stormMode?.let { prefs[Keys.STORM_MODE] = it.name }
            rainDensity?.let { prefs[Keys.RAIN_DENSITY] = it.coerceIn(0.1f, 1.0f) }
            lightningEnabled?.let { prefs[Keys.LIGHTNING_ENABLED] = it }
            stormFogEnabled?.let { prefs[Keys.STORM_FOG_ENABLED] = it }
        }
    }

    suspend fun updateLayerConfig(
        pageCount: Int? = null,
        layerTransition: LayerTransitionStyle? = null,
        showDock: Boolean? = null,
        showRecentsInDock: Boolean? = null
    ) {
        context.launcherDataStore.edit { prefs ->
            pageCount?.let { prefs[Keys.PAGE_COUNT] = it.coerceIn(1, 8) }
            layerTransition?.let { prefs[Keys.LAYER_TRANSITION] = it.name }
            showDock?.let { prefs[Keys.SHOW_DOCK] = it }
            showRecentsInDock?.let { prefs[Keys.SHOW_RECENTS_IN_DOCK] = it }
        }
    }

    suspend fun updateIconStyle(
        shape: IconShapeStyle? = null,
        sizeDp: Int? = null,
        frameBackdrop: Boolean? = null,
        showLabels: Boolean? = null,
        portraitColumns: Int? = null,
        landscapeColumns: Int? = null
    ) {
        context.launcherDataStore.edit { prefs ->
            shape?.let { prefs[Keys.ICON_SHAPE] = it.name }
            sizeDp?.let { prefs[Keys.ICON_SIZE_DP] = it.coerceIn(40, 72) }
            frameBackdrop?.let { prefs[Keys.ICON_FRAME_BACKDROP] = it }
            showLabels?.let { prefs[Keys.SHOW_APP_LABELS] = it }
            portraitColumns?.let { prefs[Keys.PORTRAIT_COLUMNS] = it.coerceIn(3, 6) }
            landscapeColumns?.let { prefs[Keys.LANDSCAPE_COLUMNS] = it.coerceIn(3, 7) }
        }
    }

    suspend fun updateAppearance(
        accentTheme: OwlAccentTheme? = null,
        glassOpacity: Float? = null,
        nightOwlMode: Boolean? = null,
        focusPerchActive: Boolean? = null,
        cleanCanvasMode: Boolean? = null,
        safetyShieldActive: Boolean? = null,
        requireVaultLock: Boolean? = null,
        editorialCustomTitle: String? = null,
        dispatchNoteText: String? = null
    ) {
        context.launcherDataStore.edit { prefs ->
            accentTheme?.let { prefs[Keys.ACCENT_THEME] = it.name }
            glassOpacity?.let { prefs[Keys.GLASS_OPACITY] = it.coerceIn(0.15f, 0.90f) }
            nightOwlMode?.let { prefs[Keys.NIGHT_OWL_MODE] = it }
            focusPerchActive?.let { prefs[Keys.FOCUS_PERCH_ACTIVE] = it }
            cleanCanvasMode?.let { prefs[Keys.CLEAN_CANVAS_MODE] = it }
            safetyShieldActive?.let { prefs[Keys.SAFETY_SHIELD_ACTIVE] = it }
            requireVaultLock?.let { prefs[Keys.REQUIRE_VAULT_LOCK] = it }
            editorialCustomTitle?.let { prefs[Keys.EDITORIAL_TITLE] = it }
            dispatchNoteText?.let { prefs[Keys.DISPATCH_NOTE] = it }
        }
    }

    suspend fun setPlacedApps(items: List<PlacedAppItem>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.PLACED_APPS_JSON] = encodePlacedApps(items)
        }
    }

    suspend fun setDockApps(keys: List<String>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.DOCK_APPS_JSON] = encodeStringList(keys.distinct().take(5))
            prefs[Keys.DOCK_INITIALIZED] = true
        }
    }

    suspend fun setHiddenApps(keys: Set<String>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.HIDDEN_APPS] = keys
        }
    }

    suspend fun setFocusEssentialApps(keys: Set<String>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.FOCUS_ESSENTIAL_APPS] = keys
        }
    }

    suspend fun setOwlNests(nests: List<OwlNest>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.OWL_NESTS_JSON] = encodeNests(nests)
        }
    }

    suspend fun setPlacedWidgets(widgets: List<PlacedWidget>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.PLACED_WIDGETS_JSON] = encodeWidgets(widgets)
            prefs[Keys.WIDGETS_INITIALIZED] = true
        }
    }

    suspend fun setStickyTexts(texts: List<HomeStickyText>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.STICKY_TEXTS_JSON] = encodeStickyTexts(texts)
        }
    }

    suspend fun setGestureBindings(bindings: Map<GestureId, GestureBinding>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.GESTURES_JSON] = encodeGestures(bindings)
        }
    }

    suspend fun setRecentApps(entries: List<RecentAppEntry>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.RECENT_APPS_JSON] = encodeRecentApps(entries.take(16))
        }
    }

    private fun encodeStringList(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodePlacedApps(items: List<PlacedAppItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("key", item.componentKey)
                put("page", item.pageIndex)
                put("scale", item.scale.toDouble())
                put("wScale", item.widthScale.toDouble())
                put("hScale", item.heightScale.toDouble())
                put("shape", item.shapeOverride?.name ?: "")
                put("slot", item.slotIndex)
                put("lSlot", item.landscapeSlotIndex)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodePlacedApps(raw: String?): List<PlacedAppItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val key = obj.optString("key")
                    if (key.isBlank()) continue
                    val shapeRaw = obj.optString("shape")
                    val shape = if (shapeRaw.isNotBlank()) {
                        shapeRaw.toEnumOrDefault(IconShapeStyle.ORIGINAL)
                    } else null
                    val baseScale = obj.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 3.6f)
                    val wScale = obj.optDouble("wScale", baseScale.toDouble()).toFloat().coerceIn(0.5f, 3.6f)
                    val hScale = obj.optDouble("hScale", baseScale.toDouble()).toFloat().coerceIn(0.5f, 3.6f)
                    add(
                        PlacedAppItem(
                            componentKey = key,
                            pageIndex = obj.optInt("page", 0).coerceAtLeast(0),
                            scale = baseScale,
                            widthScale = wScale,
                            heightScale = hScale,
                            shapeOverride = shape,
                            slotIndex = obj.optInt("slot", -1),
                            landscapeSlotIndex = obj.optInt("lSlot", -1)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeNests(nests: List<OwlNest>): String {
        val arr = JSONArray()
        nests.forEach { nest ->
            val obj = JSONObject().apply {
                put("id", nest.id)
                put("name", nest.name)
                put("page", nest.pageIndex)
                put("size", nest.folderSize.name)
                put("shape", nest.shapeOverride?.name ?: "")
                put("gif", nest.gifPath ?: "")
                put("slot", nest.slotIndex)
                put("lSlot", nest.landscapeSlotIndex)
                put("wScale", nest.widthScale.toDouble())
                put("hScale", nest.heightScale.toDouble())
                val apps = JSONArray()
                nest.appKeys.forEach { apps.put(it) }
                put("apps", apps)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeNests(raw: String?): List<OwlNest> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val appsArr = obj.optJSONArray("apps") ?: JSONArray()
                    val appKeys = buildList {
                        for (j in 0 until appsArr.length()) {
                            val key = appsArr.optString(j)
                            if (key.isNotBlank()) add(key)
                        }
                    }
                    val shapeRaw = obj.optString("shape")
                    val shape = if (shapeRaw.isNotBlank()) {
                        shapeRaw.toEnumOrDefault(IconShapeStyle.ORIGINAL)
                    } else null
                    val gif = obj.optString("gif").takeIf { it.isNotBlank() && File(it).exists() }
                    add(
                        OwlNest(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            appKeys = appKeys,
                            pageIndex = obj.optInt("page", 0).coerceAtLeast(0),
                            folderSize = obj.optString("size").toEnumOrDefault(FolderSizeMode.SIZE_1X1),
                            shapeOverride = shape,
                            gifPath = gif,
                            slotIndex = obj.optInt("slot", -1),
                            landscapeSlotIndex = obj.optInt("lSlot", -1),
                            widthScale = obj.optDouble("wScale", 1.0).toFloat().coerceIn(0.5f, 3.6f),
                            heightScale = obj.optDouble("hScale", 1.0).toFloat().coerceIn(0.5f, 3.6f)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeRecentApps(entries: List<RecentAppEntry>): String {
        val arr = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("key", entry.componentKey)
                put("ts", entry.lastOpenedAt)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeRecentApps(raw: String?): List<RecentAppEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val key = obj.optString("key")
                    if (key.isBlank()) continue
                    add(
                        RecentAppEntry(
                            componentKey = key,
                            lastOpenedAt = obj.optLong("ts", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeWidgets(widgets: List<PlacedWidget>): String {
        val arr = JSONArray()
        widgets.forEach { w ->
            val obj = JSONObject().apply {
                put("id", w.id)
                put("kind", w.kind.name)
                put("page", w.pageIndex)
                put("appWidgetId", w.appWidgetId)
                put("providerPackage", w.providerPackage)
                put("providerClass", w.providerClass)
                put("nativeType", w.nativeType?.name ?: "")
                put("size", w.size.name)
                put("width", w.customWidthDp ?: -1)
                put("height", w.customHeightDp ?: -1)
                put("radius", w.cornerRadiusDp)
                put("gif", w.gifPath ?: "")
                put("slot", w.slotIndex)
                put("lSlot", w.landscapeSlotIndex)
                put("shape", w.shapeOverride?.name ?: "")
                put("transparent", w.transparentBackground)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeWidgets(raw: String?): List<PlacedWidget> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val kind = obj.optString("kind").toEnumOrDefault(PlacedWidgetKind.OWL_NATIVE)
                    val nativeName = obj.optString("nativeType")
                    val nativeType = if (nativeName.isNotBlank()) {
                        nativeName.toEnumOrDefault(OwlNativeWidgetType.SYSTEM_GLANCE)
                    } else null
                    val customW = obj.optInt("width", -1).takeIf { it >= 48 }
                    val customH = obj.optInt("height", -1).takeIf { it >= 48 }
                    val gif = obj.optString("gif").takeIf { it.isNotBlank() && File(it).exists() }
                    val shapeRaw = obj.optString("shape")
                    val shape = if (shapeRaw.isNotBlank()) {
                        shapeRaw.toEnumOrDefault(IconShapeStyle.ORIGINAL)
                    } else null
                    val defaultTransparent = (kind == PlacedWidgetKind.ANDROID_APPWIDGET)
                    val transparent = if (obj.has("transparent")) {
                        obj.optBoolean("transparent", defaultTransparent)
                    } else {
                        defaultTransparent
                    }
                    add(
                        PlacedWidget(
                            id = obj.optString("id"),
                            kind = kind,
                            pageIndex = obj.optInt("page", 0).coerceAtLeast(0),
                            appWidgetId = obj.optInt("appWidgetId", -1),
                            providerPackage = obj.optString("providerPackage"),
                            providerClass = obj.optString("providerClass"),
                            nativeType = nativeType,
                            size = obj.optString("size").toEnumOrDefault(WidgetSize.WIDE),
                            customWidthDp = customW,
                            customHeightDp = customH,
                            cornerRadiusDp = obj.optInt("radius", 22).coerceIn(0, 48),
                            gifPath = gif,
                            slotIndex = obj.optInt("slot", -1),
                            landscapeSlotIndex = obj.optInt("lSlot", -1),
                            shapeOverride = shape,
                            transparentBackground = transparent
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeStickyTexts(texts: List<HomeStickyText>): String {
        val arr = JSONArray()
        texts.forEach { st ->
            val obj = JSONObject().apply {
                put("page", st.pageIndex)
                put("text", st.text)
                put("x", st.xFraction.toDouble())
                put("y", st.yFraction.toDouble())
                put("size", st.fontSizeSp)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeStickyTexts(raw: String?): List<HomeStickyText> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val t = obj.optString("text")
                    if (t.isNotEmpty()) {
                        add(
                            HomeStickyText(
                                pageIndex = obj.optInt("page", 0).coerceAtLeast(0),
                                text = t,
                                xFraction = obj.optDouble("x", 0.5).toFloat().coerceIn(0.08f, 0.92f),
                                yFraction = obj.optDouble("y", 0.16).toFloat().coerceIn(0.06f, 0.85f),
                                fontSizeSp = obj.optInt("size", 22).coerceIn(14, 42)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeGestures(bindings: Map<GestureId, GestureBinding>): String {
        val obj = JSONObject()
        bindings.forEach { (id, binding) ->
            val entry = JSONObject().apply {
                put("action", binding.action.name)
                put("app", binding.targetAppKey ?: "")
            }
            obj.put(id.name, entry)
        }
        return obj.toString()
    }

    private fun decodeGestures(raw: String?): Map<GestureId, GestureBinding> {
        val defaults = defaultGestureBindings().toMutableMap()
        if (raw.isNullOrBlank()) return defaults
        return try {
            val obj = JSONObject(raw)
            GestureId.entries.forEach { id ->
                val entry = obj.optJSONObject(id.name)
                if (entry != null) {
                    val action = entry.optString("action").toEnumOrDefault(
                        defaults[id]?.action ?: GestureActionType.NONE
                    )
                    val appKey = entry.optString("app").takeIf { it.isNotBlank() }
                    defaults[id] = GestureBinding(action = action, targetAppKey = appKey)
                }
            }
            defaults
        } catch (_: Exception) {
            defaults
        }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
        if (this.isNullOrBlank()) return default
        return try {
            enumValueOf<T>(this)
        } catch (_: Exception) {
            default
        }
    }
}
