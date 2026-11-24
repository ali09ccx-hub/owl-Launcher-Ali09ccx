package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.FolderSizeMode
import com.example.model.IconShapeStyle
import com.example.model.OwlNest
import com.example.model.PlacedAppItem
import com.example.model.PlacedWidget
import com.example.ui.ItemCustomizationTarget
import com.example.ui.LauncherUiState
import kotlin.math.roundToInt

@Composable
fun AppContextActionDialog(
    app: AppInfo,
    uiState: LauncherUiState,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onTogglePinHome: () -> Unit,
    onCustomizePlacedApp: (PlacedAppItem) -> Unit,
    onTogglePinDock: () -> Unit,
    onAddToNest: () -> Unit,
    onToggleFocusEssential: () -> Unit,
    onToggleVault: () -> Unit,
    onOpenInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val prefs = uiState.prefs
    val placedItem = prefs.placedApps.firstOrNull { it.componentKey == app.componentKey }
    val isPinnedHome = placedItem != null
    val isPinnedDock = app.componentKey in prefs.dockAppKeys
    val isHidden = app.componentKey in prefs.hiddenAppKeys
    val isFocusEssential = app.componentKey in prefs.focusEssentialAppKeys

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UnalteredAppIconArtwork(
                    app = app,
                    iconShape = placedItem?.shapeOverride ?: prefs.iconShape,
                    iconSize = 42.dp,
                    showFrameBackdrop = prefs.showIconFrameBackdrop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MenuActionRow(
                    icon = Icons.Filled.Launch,
                    label = stringResource(R.string.action_open),
                    onClick = { onLaunch(); onDismiss() }
                )
                MenuActionRow(
                    icon = Icons.Filled.PushPin,
                    label = stringResource(
                        if (isPinnedHome) R.string.action_unpin_home else R.string.action_pin_home
                    ),
                    onClick = { onTogglePinHome(); onDismiss() }
                )
                if (placedItem != null) {
                    MenuActionRow(
                        icon = Icons.Filled.Tune,
                        label = stringResource(R.string.action_customize_item),
                        onClick = { onCustomizePlacedApp(placedItem) }
                    )
                }
                MenuActionRow(
                    icon = Icons.Filled.Dock,
                    label = stringResource(
                        if (isPinnedDock) R.string.action_unpin_dock else R.string.action_pin_dock
                    ),
                    onClick = { onTogglePinDock(); onDismiss() }
                )
                MenuActionRow(
                    icon = Icons.Filled.FolderSpecial,
                    label = stringResource(R.string.action_add_to_nest),
                    onClick = onAddToNest
                )
                MenuActionRow(
                    icon = Icons.Filled.CenterFocusStrong,
                    label = stringResource(
                        if (isFocusEssential) R.string.action_focus_remove else R.string.action_focus_allow
                    ),
                    onClick = { onToggleFocusEssential(); onDismiss() }
                )
                MenuActionRow(
                    icon = if (isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    label = stringResource(
                        if (isHidden) R.string.action_unhide_vault else R.string.action_hide_vault
                    ),
                    onClick = { onToggleVault(); onDismiss() }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MenuActionRow(
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.action_app_info),
                    onClick = { onOpenInfo(); onDismiss() }
                )
                MenuActionRow(
                    icon = Icons.Filled.DeleteOutline,
                    label = stringResource(R.string.action_uninstall),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onUninstall(); onDismiss() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCustomizationDialog(
    target: ItemCustomizationTarget,
    pageCount: Int,
    onDismiss: () -> Unit,
    onOpenBetterEditor: (ItemCustomizationTarget) -> Unit,
    onUpdateApp: (PlacedAppItem) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onUpdateNest: (OwlNest) -> Unit,
    onAttachGifToNest: (String, Uri) -> Unit,
    onDeleteNest: (String) -> Unit,
    onUpdateWidget: (PlacedWidget) -> Unit,
    onAttachGifToWidget: (String, Uri) -> Unit,
    onRemoveWidget: (String) -> Unit
) {
    val gifPickerForNest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && target is ItemCustomizationTarget.NestTarget) {
            onAttachGifToNest(target.nest.id, uri)
        }
    }

    val gifPickerForWidget = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && target is ItemCustomizationTarget.WidgetTarget) {
            onAttachGifToWidget(target.widget.id, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (target) {
                    is ItemCustomizationTarget.AppTarget -> target.app.label
                    is ItemCustomizationTarget.NestTarget -> target.nest.name
                    is ItemCustomizationTarget.WidgetTarget -> stringResource(R.string.item_editor_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenBetterEditor(target) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("better_editor_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.better_editor_btn))
                }

                when (target) {
                    is ItemCustomizationTarget.AppTarget -> {
                        val item = target.item
                        Text(
                            text = stringResource(R.string.item_scale_label, item.scale),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = item.scale,
                            onValueChange = { onUpdateApp(item.copy(scale = it)) },
                            valueRange = 0.75f..1.55f
                        )

                        Text(
                            text = stringResource(R.string.item_shape_override),
                            style = MaterialTheme.typography.labelLarge
                        )
                        ShapePickerChips(
                            selectedShape = item.shapeOverride,
                            onSelect = { onUpdateApp(item.copy(shapeOverride = it)) }
                        )

                        if (pageCount > 1) {
                            LayerMoveRow(
                                currentPage = item.pageIndex,
                                pageCount = pageCount,
                                onMove = { newPage -> onUpdateApp(item.copy(pageIndex = newPage)) }
                            )
                        }
                    }

                    is ItemCustomizationTarget.NestTarget -> {
                        val nest = target.nest
                        var folderName by remember(nest.id) { mutableStateOf(nest.name) }
                        OutlinedTextField(
                            value = folderName,
                            onValueChange = { nextName ->
                                folderName = nextName
                                if (nextName.isNotBlank()) {
                                    onUpdateNest(nest.copy(name = nextName))
                                }
                            },
                            label = { Text(stringResource(R.string.nest_rename_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nest_rename_input")
                        )

                        Text(
                            text = stringResource(R.string.folder_style_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                FolderSizeMode.SIZE_1X1 to R.string.folder_size_1x1,
                                FolderSizeMode.SIZE_2X1 to R.string.folder_size_2x1,
                                FolderSizeMode.SIZE_2X2 to R.string.folder_size_2x2
                            ).forEach { (mode, resId) ->
                                FilterChip(
                                    selected = nest.folderSize == mode,
                                    onClick = { onUpdateNest(nest.copy(folderSize = mode)) },
                                    label = { Text(stringResource(resId)) }
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.item_shape_override),
                            style = MaterialTheme.typography.labelLarge
                        )
                        ShapePickerChips(
                            selectedShape = nest.shapeOverride,
                            onSelect = { onUpdateNest(nest.copy(shapeOverride = it)) }
                        )

                        OutlinedButton(
                            onClick = {
                                gifPickerForNest.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.GifBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.attach_gif_btn))
                        }

                        if (nest.gifPath != null) {
                            TextButton(
                                onClick = { onUpdateNest(nest.copy(gifPath = null)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.clear_gif_btn))
                            }
                        }

                        if (pageCount > 1) {
                            LayerMoveRow(
                                currentPage = nest.pageIndex,
                                pageCount = pageCount,
                                onMove = { newPage -> onUpdateNest(nest.copy(pageIndex = newPage)) }
                            )
                        }
                    }

                    is ItemCustomizationTarget.WidgetTarget -> {
                        val w = target.widget
                        val currentW = (w.customWidthDp ?: 220).toFloat()
                        Text(
                            text = stringResource(R.string.widget_width_label, currentW.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = currentW,
                            onValueChange = { onUpdateWidget(w.copy(customWidthDp = it.roundToInt())) },
                            valueRange = 52f..330f
                        )

                        val currentH = (w.customHeightDp ?: 124).toFloat()
                        Text(
                            text = stringResource(R.string.widget_height_label, currentH.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = currentH,
                            onValueChange = { onUpdateWidget(w.copy(customHeightDp = it.roundToInt())) },
                            valueRange = 52f..340f
                        )

                        Text(
                            text = stringResource(R.string.item_shape_override),
                            style = MaterialTheme.typography.labelLarge
                        )
                        ShapePickerChips(
                            selectedShape = w.shapeOverride,
                            onSelect = { onUpdateWidget(w.copy(shapeOverride = it)) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateWidget(w.copy(transparentBackground = !w.transparentBackground)) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.widget_transparent_bg_toggle),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = w.transparentBackground,
                                onCheckedChange = { onUpdateWidget(w.copy(transparentBackground = it)) }
                            )
                        }

                        Text(
                            text = stringResource(R.string.widget_radius_label, w.cornerRadiusDp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = w.cornerRadiusDp.toFloat(),
                            onValueChange = { onUpdateWidget(w.copy(cornerRadiusDp = it.roundToInt())) },
                            valueRange = 4f..36f
                        )

                        OutlinedButton(
                            onClick = {
                                gifPickerForWidget.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.GifBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.attach_gif_btn))
                        }

                        if (w.gifPath != null) {
                            TextButton(
                                onClick = { onUpdateWidget(w.copy(gifPath = null)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.clear_gif_btn))
                            }
                        }

                        if (pageCount > 1) {
                            LayerMoveRow(
                                currentPage = w.pageIndex,
                                pageCount = pageCount,
                                onMove = { newPage -> onUpdateWidget(w.copy(pageIndex = newPage)) }
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                when (target) {
                    is ItemCustomizationTarget.AppTarget -> {
                        onRemoveApp(target.app)
                        onDismiss()
                    }
                    is ItemCustomizationTarget.NestTarget -> onDeleteNest(target.nest.id)
                    is ItemCustomizationTarget.WidgetTarget -> onRemoveWidget(target.widget.id)
                }
            }) {
                Text(
                    text = stringResource(R.string.widget_remove),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.done_editing))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapePickerChips(
    selectedShape: IconShapeStyle?,
    onSelect: (IconShapeStyle?) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = selectedShape == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.shape_inherit)) }
        )
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
                selected = selectedShape == shape,
                onClick = { onSelect(shape) },
                label = { Text(stringResource(resId)) }
            )
        }
    }
}

@Composable
private fun LayerMoveRow(
    currentPage: Int,
    pageCount: Int,
    onMove: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onMove((currentPage - 1).coerceAtLeast(0)) },
            enabled = currentPage > 0,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.move_to_prev_page), maxLines = 1)
        }
        OutlinedButton(
            onClick = { onMove((currentPage + 1).coerceAtMost(pageCount - 1)) },
            enabled = currentPage < pageCount - 1,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.move_to_next_page), maxLines = 1)
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@Composable
fun AddToNestDialog(
    app: AppInfo,
    nests: List<OwlNest>,
    onDismiss: () -> Unit,
    onSelectNest: (String) -> Unit,
    onCreateNewNest: (String) -> Unit
) {
    var newNestName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_nest_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nests.forEach { nest ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectNest(nest.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(nest.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                OutlinedTextField(
                    value = newNestName,
                    onValueChange = { newNestName = it },
                    placeholder = { Text(stringResource(R.string.nest_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { onCreateNewNest(newNestName) },
                    enabled = newNestName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.nest_create_btn))
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

@Composable
fun CreateNestDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_nest)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.nest_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_nest_input")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("create_nest_confirm")
            ) {
                Text(stringResource(R.string.nest_create_btn))
            }
        }
    )
}

@Composable
fun DispatchNoteEditorDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.owl_widget_note_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dispatch_note_input")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                modifier = Modifier.testTag("dispatch_note_save")
            ) {
                Text(stringResource(R.string.save))
            }
        }
    )
}
