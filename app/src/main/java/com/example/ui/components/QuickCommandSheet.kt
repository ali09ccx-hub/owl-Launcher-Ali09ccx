package com.example.ui.components

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.ui.LauncherUiState
import com.example.ui.theme.MidnightDeep

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCommandSheet(
    uiState: LauncherUiState,
    onDismiss: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleNightOwl: () -> Unit,
    onToggleFocusPerch: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenSettingsStudio: () -> Unit,
    onOpenSystemSettings: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    var query by rememberSaveable { mutableStateOf("") }
    val prefs = uiState.prefs

    val matches = remember(query, uiState.visibleAppsInDrawer) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            uiState.visibleAppsInDrawer.take(8)
        } else {
            uiState.visibleAppsInDrawer.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }.take(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightDeep.copy(alpha = 0.92f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("quick_command_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.quick_toolbox_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_command_search")
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = prefs.nightOwlMode,
                    onClick = onToggleNightOwl,
                    label = { Text(stringResource(R.string.night_owl_mode)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = prefs.focusPerchActive,
                    onClick = onToggleFocusPerch,
                    label = { Text(stringResource(R.string.focus_perch_mode)) },
                    leadingIcon = {
                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = uiState.deviceStatus.torchEnabled,
                    onClick = onToggleFlashlight,
                    label = { Text(stringResource(R.string.flashlight_label)) },
                    leadingIcon = {
                        Icon(
                            if (uiState.deviceStatus.torchEnabled) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = onOpenSettingsStudio,
                    label = { Text(stringResource(R.string.settings_title)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = { onOpenSystemSettings(Settings.ACTION_WIFI_SETTINGS) },
                    label = { Text(stringResource(R.string.open_wifi_settings)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = { onOpenSystemSettings(Settings.ACTION_DISPLAY_SETTINGS) },
                    label = { Text(stringResource(R.string.open_display_settings)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Brightness6, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = { onOpenSystemSettings(Settings.ACTION_SETTINGS) },
                    label = { Text(stringResource(R.string.open_device_settings)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matches, key = { it.componentKey }) { app ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunchApp(app) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UnalteredAppIconArtwork(
                                app = app,
                                iconShape = prefs.iconShape,
                                iconSize = 40.dp,
                                showFrameBackdrop = prefs.showIconFrameBackdrop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
