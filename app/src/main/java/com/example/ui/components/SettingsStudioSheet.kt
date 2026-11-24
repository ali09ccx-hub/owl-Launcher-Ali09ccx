package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.GestureActionType
import com.example.model.GestureBinding
import com.example.model.GestureId
import com.example.model.IconShapeStyle
import com.example.model.LayerTransitionStyle
import com.example.model.OwlAccentTheme
import com.example.model.StormMode
import com.example.model.WallpaperConfig
import com.example.model.WallpaperScaleMode
import com.example.ui.LauncherUiState
import com.example.ui.theme.MidnightDeep
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsStudioSheet(
    uiState: LauncherUiState,
    initialTab: Int,
    systemIsLandscape: Boolean,
    onDismiss: () -> Unit,
    onSetPreviewLandscape: (Boolean?) -> Unit,
    onRequestSetDefaultLauncher: () -> Unit,
    onPickPortraitWallpaper: (Uri) -> Unit,
    onPickLandscapeWallpaper: (Uri) -> Unit,
    onResetWallpaper: (Boolean) -> Unit,
    onUpdateWallpaperConfig: (Boolean, WallpaperConfig) -> Unit,
    onSetSyncSystemWallpaper: (Boolean) -> Unit,
    onApplySystemWallpaperNow: (Boolean) -> Unit,
    onUpdateStormConfig: (StormMode?, Float?, Boolean?, Boolean?) -> Unit,
    onUpdateLayerConfig: (LayerTransitionStyle?, Boolean?, Boolean?) -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onUpdateIconStyle: (IconShapeStyle?, Int?, Boolean?, Boolean?, Int?, Int?) -> Unit,
    onUpdateAppearance: (OwlAccentTheme?, Float?, Boolean?, String?) -> Unit,
    onUpdateGestureBinding: (GestureId, GestureBinding) -> Unit,
    onOpenCreateNest: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var editingLandscapeWallpaper by rememberSaveable { mutableStateOf(systemIsLandscape) }

    LaunchedEffect(selectedTab, editingLandscapeWallpaper) {
        if (selectedTab == 1) {
            onSetPreviewLandscape(editingLandscapeWallpaper)
        } else {
            onSetPreviewLandscape(null)
        }
    }

    val portraitPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPickPortraitWallpaper(it) }
    }

    val landscapePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPickLandscapeWallpaper(it) }
    }

    var editingGestureId by remember { mutableStateOf<GestureId?>(null) }
    var pickingAppForGesture by remember { mutableStateOf<GestureId?>(null) }

    val prefs = uiState.prefs
    val activeWallpaperCfg = if (editingLandscapeWallpaper) {
        prefs.landscapeWallpaper
    } else {
        prefs.portraitWallpaper
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightDeep.copy(alpha = 0.92f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("settings_studio_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("settings_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val tabTitles = listOf(
                R.string.settings_tab_layers,
                R.string.settings_tab_wallpaper,
                R.string.settings_tab_icons,
                R.string.settings_tab_gestures
            )
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp
            ) {
                tabTitles.forEachIndexed { idx, titleRes ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = stringResource(titleRes),
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("settings_tab_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                // TAB 0: Home, Layers & Owl Signature Features
                0 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "sec_default_launcher") {
                        StudioSectionCard(title = stringResource(R.string.settings_section_system)) {
                            if (uiState.isDefaultLauncher) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.default_launcher_active),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.set_default_launcher_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onRequestSetDefaultLauncher,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.set_default_launcher_btn))
                                }
                            }
                        }
                    }

                    item(key = "sec_layers") {
                        StudioSectionCard(title = stringResource(R.string.settings_section_layers)) {
                            Text(
                                text = stringResource(
                                    R.string.page_indicator_format,
                                    uiState.currentPage + 1,
                                    prefs.pageCount
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onAddPage,
                                    enabled = prefs.pageCount < 8,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.home_menu_add_page), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onRemovePage,
                                    enabled = prefs.pageCount > 1,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.home_menu_remove_page), maxLines = 1)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.transition_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    LayerTransitionStyle.SLIDE to R.string.transition_slide,
                                    LayerTransitionStyle.DEPTH_PARALLAX to R.string.transition_depth,
                                    LayerTransitionStyle.FADE to R.string.transition_fade,
                                    LayerTransitionStyle.TILT to R.string.transition_tilt
                                ).forEach { (style, resId) ->
                                    FilterChip(
                                        selected = prefs.layerTransition == style,
                                        onClick = { onUpdateLayerConfig(style, null, null) },
                                        label = { Text(stringResource(resId)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            StudioSwitchRow(
                                label = stringResource(R.string.show_dock_toggle),
                                checked = prefs.showDock,
                                onCheckedChange = { onUpdateLayerConfig(null, it, null) }
                            )
                            StudioSwitchRow(
                                label = stringResource(R.string.show_recents_dock_toggle),
                                checked = prefs.showRecentsInDock,
                                onCheckedChange = { onUpdateLayerConfig(null, null, it) }
                            )
                        }
                    }

                    item(key = "sec_features") {
                        StudioSectionCard(title = stringResource(R.string.settings_section_features)) {
                            Text(
                                text = stringResource(R.string.accent_theme_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    OwlAccentTheme.AMBER to R.string.accent_amber,
                                    OwlAccentTheme.INDIGO to R.string.accent_indigo,
                                    OwlAccentTheme.SAGE to R.string.accent_sage,
                                    OwlAccentTheme.DYNAMIC to R.string.accent_dynamic
                                ).forEach { (theme, resId) ->
                                    FilterChip(
                                        selected = prefs.accentTheme == theme,
                                        onClick = { onUpdateAppearance(theme, null, null, null) },
                                        label = { Text(stringResource(resId)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LabeledSlider(
                                label = stringResource(
                                    R.string.glass_opacity_label,
                                    (prefs.glassOpacity * 100).roundToInt()
                                ),
                                value = prefs.glassOpacity,
                                valueRange = 0.15f..0.90f,
                                onValueChange = { onUpdateAppearance(null, it, null, null) }
                            )

                            StudioSwitchRow(
                                label = stringResource(R.string.vault_require_lock),
                                checked = prefs.requireVaultDeviceLock,
                                onCheckedChange = { onUpdateAppearance(null, null, it, null) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenCreateNest,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.FolderSpecial,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.create_nest), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onOpenWidgetPicker,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.Widgets,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.add_widget_desc), maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // TAB 1: Living Owl Storm Engine & Dual Orientation Wallpapers
                1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "sec_storm") {
                        StudioSectionCard(title = stringResource(R.string.settings_section_storm)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Thunderstorm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.storm_mode_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    StormMode.OFF to R.string.storm_off,
                                    StormMode.DRIZZLE to R.string.storm_drizzle,
                                    StormMode.THUNDER_STORM to R.string.storm_thunder
                                ).forEach { (mode, resId) ->
                                    FilterChip(
                                        selected = prefs.stormMode == mode,
                                        onClick = { onUpdateStormConfig(mode, null, null, null) },
                                        label = { Text(stringResource(resId)) },
                                        modifier = Modifier.testTag("storm_${mode.name}")
                                    )
                                }
                            }

                            if (prefs.stormMode != StormMode.OFF) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LabeledSlider(
                                    label = stringResource(
                                        R.string.storm_rain_density,
                                        (prefs.rainDensity * 100).roundToInt()
                                    ),
                                    value = prefs.rainDensity,
                                    valueRange = 0.2f..1.0f,
                                    onValueChange = { onUpdateStormConfig(null, it, null, null) }
                                )

                                StudioSwitchRow(
                                    label = stringResource(R.string.storm_lightning_toggle),
                                    checked = prefs.lightningEnabled,
                                    onCheckedChange = { onUpdateStormConfig(null, null, it, null) }
                                )
                                StudioSwitchRow(
                                    label = stringResource(R.string.storm_fog_toggle),
                                    checked = prefs.stormFogEnabled,
                                    onCheckedChange = { onUpdateStormConfig(null, null, null, it) }
                                )
                            }
                        }
                    }

                    item(key = "sec_wallpapers") {
                        StudioSectionCard(title = stringResource(R.string.settings_section_wallpapers)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !editingLandscapeWallpaper,
                                    onClick = { editingLandscapeWallpaper = false },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    icon = {
                                        Icon(
                                            Icons.Filled.PhoneAndroid,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("seg_portrait_wallpaper")
                                ) {
                                    Text(stringResource(R.string.preview_portrait), maxLines = 1)
                                }
                                SegmentedButton(
                                    selected = editingLandscapeWallpaper,
                                    onClick = { editingLandscapeWallpaper = true },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    icon = {
                                        Icon(
                                            Icons.Filled.ScreenRotation,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("seg_landscape_wallpaper")
                                ) {
                                    Text(stringResource(R.string.preview_landscape), maxLines = 1)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(if (editingLandscapeWallpaper) 150.dp else 88.dp)
                                        .aspectRatio(if (editingLandscapeWallpaper) 16f / 9f else 9f / 16f)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    WallpaperCanvas(
                                        isLandscape = editingLandscapeWallpaper,
                                        portraitConfig = prefs.portraitWallpaper,
                                        landscapeConfig = prefs.landscapeWallpaper,
                                        nightOwlMode = prefs.nightOwlMode,
                                        stormMode = prefs.stormMode,
                                        rainDensity = prefs.rainDensity,
                                        lightningEnabled = prefs.lightningEnabled,
                                        stormFogEnabled = prefs.stormFogEnabled
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (editingLandscapeWallpaper) {
                                                R.string.wallpaper_landscape_title
                                            } else {
                                                R.string.wallpaper_portrait_title
                                            }
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = stringResource(
                                            if (activeWallpaperCfg.customFilePath != null) {
                                                R.string.wallpaper_custom_active
                                            } else {
                                                R.string.wallpaper_default_active
                                            }
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = {
                                            val req = PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                            if (editingLandscapeWallpaper) {
                                                landscapePicker.launch(req)
                                            } else {
                                                portraitPicker.launch(req)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pick_wallpaper_btn")
                                    ) {
                                        Icon(
                                            Icons.Filled.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.wallpaper_pick))
                                    }

                                    if (activeWallpaperCfg.customFilePath != null) {
                                        OutlinedButton(
                                            onClick = { onResetWallpaper(editingLandscapeWallpaper) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Filled.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.wallpaper_reset))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.scale_mode_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    WallpaperScaleMode.CROP to R.string.scale_crop,
                                    WallpaperScaleMode.FIT to R.string.scale_fit,
                                    WallpaperScaleMode.STRETCH to R.string.scale_stretch
                                ).forEach { (mode, labelRes) ->
                                    FilterChip(
                                        selected = activeWallpaperCfg.scaleMode == mode,
                                        onClick = {
                                            onUpdateWallpaperConfig(
                                                editingLandscapeWallpaper,
                                                activeWallpaperCfg.copy(scaleMode = mode)
                                            )
                                        },
                                        label = { Text(stringResource(labelRes)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LabeledSlider(
                                label = stringResource(R.string.zoom_label, activeWallpaperCfg.zoom),
                                value = activeWallpaperCfg.zoom,
                                valueRange = 1.0f..2.5f,
                                onValueChange = {
                                    onUpdateWallpaperConfig(
                                        editingLandscapeWallpaper,
                                        activeWallpaperCfg.copy(zoom = it)
                                    )
                                }
                            )
                            LabeledSlider(
                                label = stringResource(
                                    R.string.offset_x_label,
                                    (activeWallpaperCfg.offsetX * 100).roundToInt()
                                ),
                                value = activeWallpaperCfg.offsetX,
                                valueRange = -1f..1f,
                                onValueChange = {
                                    onUpdateWallpaperConfig(
                                        editingLandscapeWallpaper,
                                        activeWallpaperCfg.copy(offsetX = it)
                                    )
                                }
                            )
                            LabeledSlider(
                                label = stringResource(
                                    R.string.offset_y_label,
                                    (activeWallpaperCfg.offsetY * 100).roundToInt()
                                ),
                                value = activeWallpaperCfg.offsetY,
                                valueRange = -1f..1f,
                                onValueChange = {
                                    onUpdateWallpaperConfig(
                                        editingLandscapeWallpaper,
                                        activeWallpaperCfg.copy(offsetY = it)
                                    )
                                }
                            )
                            LabeledSlider(
                                label = stringResource(
                                    R.string.dim_label,
                                    (activeWallpaperCfg.dimAlpha * 100).roundToInt()
                                ),
                                value = activeWallpaperCfg.dimAlpha,
                                valueRange = 0f..0.75f,
                                onValueChange = {
                                    onUpdateWallpaperConfig(
                                        editingLandscapeWallpaper,
                                        activeWallpaperCfg.copy(dimAlpha = it)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            StudioSwitchRow(
                                label = stringResource(R.string.sync_system_wallpaper_toggle),
                                checked = prefs.syncSystemWallpaper,
                                onCheckedChange = onSetSyncSystemWallpaper
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onApplySystemWallpaperNow(editingLandscapeWallpaper) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_system_wallpaper_btn")
                            ) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.apply_system_wallpaper_btn))
                            }
                        }
                    }
                }

                // TAB 2: Icon Shape, Size & Grid Studio
                2 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "sec_icons") {
                        StudioSectionCard(title = stringResource(R.string.settings_section_icons)) {
                            Text(
                                text = stringResource(R.string.icon_shape_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val sampleApps = uiState.installedApps.take(4)
                            if (sampleApps.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    sampleApps.forEach { app ->
                                        UnalteredAppIconArtwork(
                                            app = app,
                                            iconShape = prefs.iconShape,
                                            iconSize = prefs.iconSizeDp.dp,
                                            showFrameBackdrop = prefs.showIconFrameBackdrop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    IconShapeStyle.ORIGINAL to R.string.shape_original,
                                    IconShapeStyle.SQUARE to R.string.shape_square,
                                    IconShapeStyle.CIRCLE to R.string.shape_circle,
                                    IconShapeStyle.TRIANGLE to R.string.shape_triangle,
                                    IconShapeStyle.SQUIRCLE to R.string.shape_squircle,
                                    IconShapeStyle.ROUNDED to R.string.shape_rounded,
                                    IconShapeStyle.FEATHER to R.string.shape_feather,
                                    IconShapeStyle.SHIELD to R.string.shape_shield
                                ).forEach { (shape, resId) ->
                                    FilterChip(
                                        selected = prefs.iconShape == shape,
                                        onClick = { onUpdateIconStyle(shape, null, null, null, null, null) },
                                        label = { Text(stringResource(resId)) },
                                        modifier = Modifier.testTag("shape_${shape.name}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LabeledSlider(
                                label = stringResource(R.string.icon_size_label, prefs.iconSizeDp),
                                value = prefs.iconSizeDp.toFloat(),
                                valueRange = 40f..68f,
                                onValueChange = {
                                    onUpdateIconStyle(null, it.roundToInt(), null, null, null, null)
                                }
                            )

                            LabeledSlider(
                                label = stringResource(R.string.portrait_columns_label, prefs.portraitColumns),
                                value = prefs.portraitColumns.toFloat(),
                                valueRange = 3f..6f,
                                steps = 2,
                                onValueChange = {
                                    onUpdateIconStyle(null, null, null, null, it.roundToInt(), null)
                                }
                            )

                            LabeledSlider(
                                label = stringResource(R.string.landscape_columns_label, prefs.landscapeColumns),
                                value = prefs.landscapeColumns.toFloat(),
                                valueRange = 3f..7f,
                                steps = 3,
                                onValueChange = {
                                    onUpdateIconStyle(null, null, null, null, null, it.roundToInt())
                                }
                            )

                            StudioSwitchRow(
                                label = stringResource(R.string.icon_frame_backdrop),
                                checked = prefs.showIconFrameBackdrop,
                                onCheckedChange = {
                                    onUpdateIconStyle(null, null, it, null, null, null)
                                }
                            )

                            StudioSwitchRow(
                                label = stringResource(R.string.show_app_labels),
                                checked = prefs.showAppLabels,
                                onCheckedChange = {
                                    onUpdateIconStyle(null, null, null, it, null, null)
                                }
                            )
                        }
                    }
                }

                // TAB 3: 24 Multi-Touch & Edge Gestures Studio
                3 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "gestures_intro") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Gesture,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.gestures_intro),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(GestureId.entries, key = { it.name }) { gestureId ->
                        val binding = prefs.gestureBindings[gestureId]
                            ?: GestureBinding(GestureActionType.NONE)
                        val boundAppLabel = binding.targetAppKey?.let { uiState.findApp(it)?.label }
                        val actionSummary = if (binding.action == GestureActionType.LAUNCH_APP && boundAppLabel != null) {
                            stringResource(R.string.ga_launch_app_named, boundAppLabel)
                        } else {
                            stringResource(gestureActionLabelRes(binding.action))
                        }

                        Surface(
                            onClick = { editingGestureId = gestureId },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gesture_row_${gestureId.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(gestureLabelRes(gestureId)),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = actionSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (binding.action == GestureActionType.NONE) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                Icon(
                                    imageVector = if (binding.action == GestureActionType.LAUNCH_APP) {
                                        Icons.Filled.Apps
                                    } else {
                                        Icons.Filled.Gesture
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Gesture Action Picker Dialog
    editingGestureId?.let { gId ->
        AlertDialog(
            onDismissRequest = { editingGestureId = null },
            title = {
                Text(
                    text = "${stringResource(R.string.gesture_pick_action_title)} — ${stringResource(gestureLabelRes(gId))}"
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(GestureActionType.entries, key = { it.name }) { action ->
                        Surface(
                            onClick = {
                                if (action == GestureActionType.LAUNCH_APP) {
                                    editingGestureId = null
                                    pickingAppForGesture = gId
                                } else {
                                    onUpdateGestureBinding(gId, GestureBinding(action))
                                    editingGestureId = null
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(gestureActionLabelRes(action)),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingGestureId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Gesture Target App Picker Dialog
    pickingAppForGesture?.let { gId ->
        GestureAppPickerDialog(
            apps = uiState.installedApps,
            onDismiss = { pickingAppForGesture = null },
            onPickApp = { app ->
                onUpdateGestureBinding(
                    gId,
                    GestureBinding(GestureActionType.LAUNCH_APP, targetAppKey = app.componentKey)
                )
                pickingAppForGesture = null
            }
        )
    }
}

@Composable
private fun GestureAppPickerDialog(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onPickApp: (AppInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gesture_pick_app_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.componentKey }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPickApp(app) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UnalteredAppIconArtwork(
                                app = app,
                                iconShape = IconShapeStyle.ORIGINAL,
                                iconSize = 36.dp,
                                showFrameBackdrop = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@StringRes
private fun gestureLabelRes(id: GestureId): Int = when (id) {
    GestureId.ONE_F_SWIPE_UP -> R.string.g_1f_swipe_up
    GestureId.ONE_F_SWIPE_DOWN -> R.string.g_1f_swipe_down
    GestureId.ONE_F_DOUBLE_TAP -> R.string.g_1f_double_tap
    GestureId.ONE_F_TRIPLE_TAP -> R.string.g_1f_triple_tap
    GestureId.TWO_F_SWIPE_UP -> R.string.g_2f_swipe_up
    GestureId.TWO_F_SWIPE_DOWN -> R.string.g_2f_swipe_down
    GestureId.TWO_F_SWIPE_LEFT -> R.string.g_2f_swipe_left
    GestureId.TWO_F_SWIPE_RIGHT -> R.string.g_2f_swipe_right
    GestureId.TWO_F_TAP -> R.string.g_2f_tap
    GestureId.TWO_F_DOUBLE_TAP -> R.string.g_2f_double_tap
    GestureId.TWO_F_PINCH_IN -> R.string.g_2f_pinch_in
    GestureId.TWO_F_SPREAD_OUT -> R.string.g_2f_spread_out
    GestureId.TWO_F_ROTATE_CW -> R.string.g_2f_rotate_cw
    GestureId.TWO_F_ROTATE_CCW -> R.string.g_2f_rotate_ccw
    GestureId.THREE_F_SWIPE_UP -> R.string.g_3f_swipe_up
    GestureId.THREE_F_SWIPE_DOWN -> R.string.g_3f_swipe_down
    GestureId.THREE_F_SWIPE_LEFT -> R.string.g_3f_swipe_left
    GestureId.THREE_F_SWIPE_RIGHT -> R.string.g_3f_swipe_right
    GestureId.THREE_F_TAP -> R.string.g_3f_tap
    GestureId.FOUR_F_SWIPE_UP -> R.string.g_4f_swipe_up
    GestureId.FOUR_F_SWIPE_DOWN -> R.string.g_4f_swipe_down
    GestureId.FOUR_F_TAP -> R.string.g_4f_tap
    GestureId.EIGHT_F_TAP -> R.string.g_8f_tap
    GestureId.EDGE_LEFT_IN -> R.string.g_edge_left_in
    GestureId.EDGE_RIGHT_IN -> R.string.g_edge_right_in
}

@StringRes
private fun gestureActionLabelRes(action: GestureActionType): Int = when (action) {
    GestureActionType.NONE -> R.string.ga_none
    GestureActionType.OPEN_DRAWER -> R.string.ga_open_drawer
    GestureActionType.OPEN_RECENTS -> R.string.ga_open_recents
    GestureActionType.TOGGLE_EDIT -> R.string.ga_toggle_edit
    GestureActionType.OPEN_WIDGETS -> R.string.ga_open_widgets
    GestureActionType.NEXT_PAGE -> R.string.ga_next_page
    GestureActionType.PREV_PAGE -> R.string.ga_prev_page
    GestureActionType.FIRST_PAGE -> R.string.ga_first_page
    GestureActionType.LAST_PAGE -> R.string.ga_last_page
    GestureActionType.OPEN_VAULT -> R.string.ga_open_vault
    GestureActionType.TOGGLE_NIGHT_OWL -> R.string.ga_toggle_night_owl
    GestureActionType.TOGGLE_FOCUS -> R.string.ga_toggle_focus
    GestureActionType.TOGGLE_CLEAN -> R.string.ga_toggle_clean
    GestureActionType.TOGGLE_SAFETY -> R.string.ga_toggle_safety
    GestureActionType.TOGGLE_TORCH -> R.string.ga_toggle_torch
    GestureActionType.OPEN_NOTE -> R.string.ga_open_note
    GestureActionType.OPEN_HOME_TYPER -> R.string.ga_open_home_typer
    GestureActionType.EXPAND_NOTIFICATIONS -> R.string.ga_expand_notifications
    GestureActionType.OPEN_DEVICE_SETTINGS -> R.string.ga_open_device_settings
    GestureActionType.LAUNCH_APP -> R.string.ga_launch_app
}

@Composable
private fun StudioSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun StudioSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
