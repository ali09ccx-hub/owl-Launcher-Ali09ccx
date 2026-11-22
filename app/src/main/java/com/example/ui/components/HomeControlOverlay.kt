package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Floating Home Screen control bar revealed ONLY on long-press of the Home Screen,
 * giving access to the Edit Pencil mode, Settings Studio, Widgets/GIFs, Folders, Gestures, and Layers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeLongPressControlBar(
    currentPage: Int,
    pageCount: Int,
    isEditMode: Boolean,
    onToggleEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWidgets: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenGestures: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("home_long_press_bar"),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.home_menu_title)} • ${
                        stringResource(R.string.page_indicator_format, currentPage + 1, pageCount)
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("home_menu_done_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.done_editing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlBarTile(
                    icon = Icons.Filled.Edit,
                    label = stringResource(R.string.home_menu_edit),
                    highlighted = isEditMode,
                    onClick = onToggleEdit,
                    testTag = "home_menu_edit_btn"
                )
                ControlBarTile(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.home_menu_settings),
                    highlighted = false,
                    onClick = onOpenSettings,
                    testTag = "home_menu_settings_btn"
                )
                ControlBarTile(
                    icon = Icons.Filled.Widgets,
                    label = stringResource(R.string.home_menu_widgets),
                    highlighted = false,
                    onClick = onOpenWidgets,
                    testTag = "home_menu_widgets_btn"
                )
                ControlBarTile(
                    icon = Icons.Filled.CreateNewFolder,
                    label = stringResource(R.string.home_menu_folder),
                    highlighted = false,
                    onClick = onCreateFolder,
                    testTag = "home_menu_folder_btn"
                )
                ControlBarTile(
                    icon = Icons.Filled.Gesture,
                    label = stringResource(R.string.home_menu_gestures),
                    highlighted = false,
                    onClick = onOpenGestures,
                    testTag = "home_menu_gestures_btn"
                )
                ControlBarTile(
                    icon = Icons.Filled.AddToPhotos,
                    label = stringResource(R.string.home_menu_add_page),
                    highlighted = false,
                    onClick = onAddPage,
                    testTag = "home_menu_add_page_btn"
                )
                if (pageCount > 1) {
                    ControlBarTile(
                        icon = Icons.Filled.DeleteSweep,
                        label = stringResource(R.string.home_menu_remove_page),
                        highlighted = false,
                        onClick = onRemovePage,
                        testTag = "home_menu_remove_page_btn"
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlBarTile(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    Color.White
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
