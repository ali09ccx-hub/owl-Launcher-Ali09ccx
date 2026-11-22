package com.example.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.ui.LauncherUiState
import com.example.ui.theme.AmiriEditorialFamily
import com.example.ui.theme.MidnightDeep

@Composable
fun AppDrawerSheet(
    uiState: LauncherUiState,
    onDismiss: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onVaultUnlocked: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val prefs = uiState.prefs
    val columns = if (isLandscape) {
        (prefs.landscapeColumns + 1).coerceIn(4, 7)
    } else {
        prefs.portraitColumns.coerceIn(3, 6)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val authTitle = stringResource(R.string.vault_locked_title)
    val authReason = stringResource(R.string.vault_auth_reason)
    val deviceConfirmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onVaultUnlocked(true)
        }
    }

    val unlockVaultAction = {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val intent = if (km != null && km.isDeviceSecure) {
            km.createConfirmDeviceCredentialIntent(authTitle, authReason)
        } else {
            null
        }
        if (intent != null) {
            deviceConfirmLauncher.launch(intent)
        } else {
            onVaultUnlocked(true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightDeep.copy(alpha = 0.93f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("app_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Drag Handle + Swipe-down-to-close Zone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var totalDragY = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDragY = 0f },
                            onVerticalDrag = { _, dragAmount -> totalDragY += dragAmount },
                            onDragEnd = {
                                if (totalDragY > 70f) onDismiss()
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 6.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.38f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_apps_hint),
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drawer_search_input")
                    )

                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("drawer_close_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_all_apps), maxLines = 1) },
                        modifier = Modifier.testTag("drawer_tab_all")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_categories), maxLines = 1) },
                        modifier = Modifier.testTag("drawer_tab_categories")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(stringResource(R.string.tab_vault), maxLines = 1) },
                        modifier = Modifier.testTag("drawer_tab_vault")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Allow swiping down to return Home when scrolled at top of list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(selectedTab) {
                        var dragDown = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragDown = 0f },
                            onVerticalDrag = { _, dy ->
                                val atTop = listState.firstVisibleItemIndex == 0 &&
                                    listState.firstVisibleItemScrollOffset == 0
                                if (atTop && dy > 0f) dragDown += dy
                            },
                            onDragEnd = {
                                if (dragDown > 120f) onDismiss()
                            }
                        )
                    }
            ) {
                when (selectedTab) {
                    0 -> {
                        val filtered = remember(uiState.visibleAppsInDrawer, searchQuery) {
                            if (searchQuery.isBlank()) {
                                uiState.visibleAppsInDrawer
                            } else {
                                val q = searchQuery.trim().lowercase()
                                uiState.visibleAppsInDrawer.filter {
                                    it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                                }
                            }
                        }
                        DrawerAppsGrid(
                            apps = filtered,
                            columns = columns,
                            listState = listState,
                            uiState = uiState,
                            onLaunchApp = onLaunchApp,
                            onShowAppMenu = onShowAppMenu
                        )
                    }

                    1 -> {
                        val filtered = remember(uiState.visibleAppsInDrawer, searchQuery) {
                            if (searchQuery.isBlank()) {
                                uiState.visibleAppsInDrawer
                            } else {
                                val q = searchQuery.trim().lowercase()
                                uiState.visibleAppsInDrawer.filter { it.label.lowercase().contains(q) }
                            }
                        }
                        val grouped = remember(filtered) {
                            filtered.groupBy { it.category }.toSortedMap(compareBy { it.ordinal })
                        }
                        if (grouped.isEmpty()) {
                            EmptySearchState()
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                grouped.forEach { (category, categoryApps) ->
                                    item(key = "cat_hdr_${category.name}") {
                                        EditorialSectionHeader(title = categoryTitle(category))
                                    }
                                    val rows = categoryApps.chunked(columns)
                                    itemsIndexed(
                                        items = rows,
                                        key = { idx, _ -> "cat_${category.name}_$idx" }
                                    ) { _, rowApps ->
                                        AppGridRow(
                                            rowApps = rowApps,
                                            columns = columns,
                                            uiState = uiState,
                                            onLaunchApp = onLaunchApp,
                                            onShowAppMenu = onShowAppMenu
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        val isLocked = prefs.requireVaultDeviceLock && !uiState.vaultUnlocked
                        if (isLocked) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(26.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.vault_locked_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = unlockVaultAction,
                                            modifier = Modifier.testTag("unlock_vault_btn")
                                        ) {
                                            Icon(
                                                Icons.Filled.LockOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.vault_unlock_btn))
                                        }
                                    }
                                }
                            }
                        } else {
                            val vaultApps = remember(uiState.vaultApps, searchQuery) {
                                if (searchQuery.isBlank()) {
                                    uiState.vaultApps
                                } else {
                                    val q = searchQuery.trim().lowercase()
                                    uiState.vaultApps.filter { it.label.lowercase().contains(q) }
                                }
                            }
                            if (vaultApps.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = stringResource(R.string.vault_empty_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.vault_empty_subtitle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                DrawerAppsGrid(
                                    apps = vaultApps,
                                    columns = columns,
                                    listState = listState,
                                    uiState = uiState,
                                    onLaunchApp = onLaunchApp,
                                    onShowAppMenu = onShowAppMenu
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerAppsGrid(
    apps: List<AppInfo>,
    columns: Int,
    listState: LazyListState,
    uiState: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit
) {
    if (apps.isEmpty()) {
        EmptySearchState()
        return
    }
    val rows = apps.chunked(columns)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = rows,
            key = { idx, row -> "drawer_row_${idx}_${row.firstOrNull()?.componentKey ?: idx}" }
        ) { _, rowApps ->
            AppGridRow(
                rowApps = rowApps,
                columns = columns,
                uiState = uiState,
                onLaunchApp = onLaunchApp,
                onShowAppMenu = onShowAppMenu
            )
        }
    }
}

@Composable
private fun AppGridRow(
    rowApps: List<AppInfo>,
    columns: Int,
    uiState: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit
) {
    val prefs = uiState.prefs
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (col in 0 until columns) {
            val app = rowApps.getOrNull(col)
            Box(modifier = Modifier.weight(1f)) {
                if (app != null) {
                    AppIconTile(
                        app = app,
                        iconShape = prefs.iconShape,
                        iconSize = prefs.iconSizeDp.dp,
                        showFrameBackdrop = prefs.showIconFrameBackdrop,
                        showLabel = prefs.showAppLabels,
                        isEditMode = false,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onShowAppMenu(app) }
                    )
                } else {
                    Spacer(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_apps_found),
            fontFamily = AmiriEditorialFamily,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun categoryTitle(category: AppCategory): String = when (category) {
    AppCategory.COMMUNICATION -> stringResource(R.string.cat_communication)
    AppCategory.PRODUCTIVITY -> stringResource(R.string.cat_productivity)
    AppCategory.MEDIA -> stringResource(R.string.cat_media)
    AppCategory.GAMES -> stringResource(R.string.cat_games)
    AppCategory.UTILITIES -> stringResource(R.string.cat_utilities)
    AppCategory.OTHER -> stringResource(R.string.cat_other)
}
