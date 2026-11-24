package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

enum class AppCategory {
    COMMUNICATION,
    PRODUCTIVITY,
    MEDIA,
    GAMES,
    UTILITIES,
    OTHER
}

@Immutable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: AppCategory,
    val icon: ImageBitmap? = null,
    val installTime: Long = 0L
) {
    val componentKey: String get() = "$packageName/$activityName"
}

enum class IconShapeStyle {
    ORIGINAL,
    SQUARE,
    SQUIRCLE,
    CIRCLE,
    TRIANGLE,
    ROUNDED,
    FEATHER,
    SHIELD
}

enum class WallpaperScaleMode {
    CROP,
    FIT,
    STRETCH
}

enum class StormMode {
    OFF,
    DRIZZLE,
    THUNDER_STORM
}

enum class LayerTransitionStyle {
    SLIDE,
    DEPTH_PARALLAX,
    FADE,
    TILT
}

@Immutable
data class WallpaperConfig(
    val customFilePath: String? = null,
    val version: Long = 0L,
    val scaleMode: WallpaperScaleMode = WallpaperScaleMode.CROP,
    val zoom: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val dimAlpha: Float = 0.14f
)

enum class OwlAccentTheme {
    AMBER,
    INDIGO,
    SAGE,
    DYNAMIC
}

enum class FolderSizeMode {
    SIZE_1X1,
    SIZE_2X1,
    SIZE_2X2
}

@Immutable
data class PlacedAppItem(
    val componentKey: String,
    val pageIndex: Int = 0,
    val scale: Float = 1.0f,
    val widthScale: Float = 1.0f,
    val heightScale: Float = 1.0f,
    val shapeOverride: IconShapeStyle? = null,
    val slotIndex: Int = -1,
    val landscapeSlotIndex: Int = -1
) {
    val effectiveWidthScale: Float get() = if (widthScale != 1.0f) widthScale else scale
    val effectiveHeightScale: Float get() = if (heightScale != 1.0f) heightScale else scale
}

@Immutable
data class OwlNest(
    val id: String,
    val name: String,
    val appKeys: List<String> = emptyList(),
    val pageIndex: Int = 0,
    val folderSize: FolderSizeMode = FolderSizeMode.SIZE_1X1,
    val shapeOverride: IconShapeStyle? = null,
    val gifPath: String? = null,
    val slotIndex: Int = -1,
    val landscapeSlotIndex: Int = -1,
    val widthScale: Float = 1.0f,
    val heightScale: Float = 1.0f
)

@Immutable
data class RecentAppEntry(
    val componentKey: String,
    val lastOpenedAt: Long
)

enum class WidgetSize {
    COMPACT,
    WIDE,
    TALL
}

enum class OwlNativeWidgetType {
    FOCUS_TIMER,
    DISPATCH_NOTE,
    SYSTEM_GLANCE,
    OWL_WISDOM,
    GIF_STICKER
}

enum class PlacedWidgetKind {
    ANDROID_APPWIDGET,
    OWL_NATIVE
}

@Immutable
data class PlacedWidget(
    val id: String,
    val kind: PlacedWidgetKind,
    val pageIndex: Int = 0,
    val appWidgetId: Int = -1,
    val providerPackage: String = "",
    val providerClass: String = "",
    val nativeType: OwlNativeWidgetType? = null,
    val size: WidgetSize = WidgetSize.WIDE,
    val customWidthDp: Int? = null,
    val customHeightDp: Int? = null,
    val cornerRadiusDp: Int = 22,
    val gifPath: String? = null,
    val slotIndex: Int = -1,
    val landscapeSlotIndex: Int = -1,
    val shapeOverride: IconShapeStyle? = null,
    val transparentBackground: Boolean = (kind == PlacedWidgetKind.ANDROID_APPWIDGET)
)

@Immutable
data class HomeStickyText(
    val pageIndex: Int = 0,
    val text: String = "",
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.16f,
    val fontSizeSp: Int = 22
)

enum class GestureId {
    ONE_F_SWIPE_UP,
    ONE_F_SWIPE_DOWN,
    ONE_F_DOUBLE_TAP,
    ONE_F_TRIPLE_TAP,
    TWO_F_SWIPE_UP,
    TWO_F_SWIPE_DOWN,
    TWO_F_SWIPE_LEFT,
    TWO_F_SWIPE_RIGHT,
    TWO_F_TAP,
    TWO_F_DOUBLE_TAP,
    TWO_F_PINCH_IN,
    TWO_F_SPREAD_OUT,
    TWO_F_ROTATE_CW,
    TWO_F_ROTATE_CCW,
    THREE_F_SWIPE_UP,
    THREE_F_SWIPE_DOWN,
    THREE_F_SWIPE_LEFT,
    THREE_F_SWIPE_RIGHT,
    THREE_F_TAP,
    FOUR_F_SWIPE_UP,
    FOUR_F_SWIPE_DOWN,
    FOUR_F_TAP,
    EIGHT_F_TAP,
    EDGE_LEFT_IN,
    EDGE_RIGHT_IN
}

enum class GestureActionType {
    NONE,
    OPEN_DRAWER,
    OPEN_RECENTS,
    TOGGLE_EDIT,
    OPEN_WIDGETS,
    NEXT_PAGE,
    PREV_PAGE,
    FIRST_PAGE,
    LAST_PAGE,
    OPEN_VAULT,
    TOGGLE_NIGHT_OWL,
    TOGGLE_FOCUS,
    TOGGLE_CLEAN,
    TOGGLE_SAFETY,
    TOGGLE_TORCH,
    OPEN_NOTE,
    OPEN_HOME_TYPER,
    EXPAND_NOTIFICATIONS,
    OPEN_DEVICE_SETTINGS,
    LAUNCH_APP
}

@Immutable
data class GestureBinding(
    val action: GestureActionType,
    val targetAppKey: String? = null
)

fun defaultGestureBindings(): Map<GestureId, GestureBinding> = mapOf(
    GestureId.ONE_F_SWIPE_UP to GestureBinding(GestureActionType.OPEN_DRAWER),
    GestureId.ONE_F_SWIPE_DOWN to GestureBinding(GestureActionType.EXPAND_NOTIFICATIONS),
    GestureId.ONE_F_DOUBLE_TAP to GestureBinding(GestureActionType.TOGGLE_CLEAN),
    GestureId.ONE_F_TRIPLE_TAP to GestureBinding(GestureActionType.TOGGLE_NIGHT_OWL),
    GestureId.TWO_F_SWIPE_UP to GestureBinding(GestureActionType.OPEN_RECENTS),
    GestureId.TWO_F_SWIPE_DOWN to GestureBinding(GestureActionType.EXPAND_NOTIFICATIONS),
    GestureId.TWO_F_SWIPE_LEFT to GestureBinding(GestureActionType.NEXT_PAGE),
    GestureId.TWO_F_SWIPE_RIGHT to GestureBinding(GestureActionType.PREV_PAGE),
    GestureId.TWO_F_TAP to GestureBinding(GestureActionType.TOGGLE_FOCUS),
    GestureId.TWO_F_DOUBLE_TAP to GestureBinding(GestureActionType.TOGGLE_EDIT),
    GestureId.TWO_F_PINCH_IN to GestureBinding(GestureActionType.TOGGLE_CLEAN),
    GestureId.TWO_F_SPREAD_OUT to GestureBinding(GestureActionType.OPEN_VAULT),
    GestureId.TWO_F_ROTATE_CW to GestureBinding(GestureActionType.TOGGLE_TORCH),
    GestureId.TWO_F_ROTATE_CCW to GestureBinding(GestureActionType.OPEN_WIDGETS),
    GestureId.THREE_F_SWIPE_UP to GestureBinding(GestureActionType.OPEN_DRAWER),
    GestureId.THREE_F_SWIPE_DOWN to GestureBinding(GestureActionType.TOGGLE_SAFETY),
    GestureId.THREE_F_SWIPE_LEFT to GestureBinding(GestureActionType.LAST_PAGE),
    GestureId.THREE_F_SWIPE_RIGHT to GestureBinding(GestureActionType.FIRST_PAGE),
    GestureId.THREE_F_TAP to GestureBinding(GestureActionType.TOGGLE_SAFETY),
    GestureId.FOUR_F_SWIPE_UP to GestureBinding(GestureActionType.FIRST_PAGE),
    GestureId.FOUR_F_SWIPE_DOWN to GestureBinding(GestureActionType.OPEN_DEVICE_SETTINGS),
    GestureId.FOUR_F_TAP to GestureBinding(GestureActionType.OPEN_HOME_TYPER),
    GestureId.EIGHT_F_TAP to GestureBinding(GestureActionType.OPEN_HOME_TYPER),
    GestureId.EDGE_LEFT_IN to GestureBinding(GestureActionType.PREV_PAGE),
    GestureId.EDGE_RIGHT_IN to GestureBinding(GestureActionType.NEXT_PAGE)
)

@Immutable
data class LauncherPreferencesState(
    val portraitWallpaper: WallpaperConfig = WallpaperConfig(),
    val landscapeWallpaper: WallpaperConfig = WallpaperConfig(),
    val stormMode: StormMode = StormMode.THUNDER_STORM,
    val rainDensity: Float = 0.65f,
    val lightningEnabled: Boolean = true,
    val stormFogEnabled: Boolean = true,
    val layerTransition: LayerTransitionStyle = LayerTransitionStyle.DEPTH_PARALLAX,
    val pageCount: Int = 2,
    val showDock: Boolean = true,
    val showRecentsInDock: Boolean = false,
    val syncSystemWallpaper: Boolean = true,
    val iconShape: IconShapeStyle = IconShapeStyle.ORIGINAL,
    val iconSizeDp: Int = 54,
    val showIconFrameBackdrop: Boolean = false,
    val showAppLabels: Boolean = false,
    val portraitColumns: Int = 4,
    val landscapeColumns: Int = 5,
    val glassOpacity: Float = 0.54f,
    val accentTheme: OwlAccentTheme = OwlAccentTheme.AMBER,
    val nightOwlMode: Boolean = false,
    val focusPerchActive: Boolean = false,
    val cleanCanvasMode: Boolean = false,
    val safetyShieldActive: Boolean = false,
    val requireVaultDeviceLock: Boolean = true,
    val editorialCustomTitle: String = "",
    val dispatchNoteText: String = "",
    val placedApps: List<PlacedAppItem> = emptyList(),
    val dockAppKeys: List<String> = emptyList(),
    val hiddenAppKeys: Set<String> = emptySet(),
    val focusEssentialAppKeys: Set<String> = emptySet(),
    val owlNests: List<OwlNest> = emptyList(),
    val placedWidgets: List<PlacedWidget> = emptyList(),
    val stickyTexts: List<HomeStickyText> = emptyList(),
    val recentApps: List<RecentAppEntry> = emptyList(),
    val gestureBindings: Map<GestureId, GestureBinding> = defaultGestureBindings()
)

@Immutable
data class DeviceStatus(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val freeStorageText: String = "",
    val torchSupported: Boolean = false,
    val torchEnabled: Boolean = false
)

@Immutable
data class AvailableWidgetProvider(
    val providerPackage: String,
    val providerClass: String,
    val appLabel: String,
    val widgetLabel: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val previewBitmap: ImageBitmap? = null
)
