package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.PlacedWidget
import com.example.model.WidgetSize
import com.example.widgets.OwlAppWidgetHost

@Composable
fun SystemWidgetCard(
    widget: PlacedWidget,
    widgetHost: OwlAppWidgetHost,
    glassOpacity: Float,
    isEditMode: Boolean,
    onCustomize: () -> Unit,
    onCycleSize: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val providerInfo = remember(widget.appWidgetId, widget.providerPackage, widget.providerClass) {
        widgetHost.getProviderInfoForWidgetId(widget.appWidgetId)
            ?: widgetHost.findProviderInfo(widget.providerPackage, widget.providerClass)
    }

    val widgetHeight = widget.customHeightDp?.dp ?: when (widget.size) {
        WidgetSize.COMPACT -> 112.dp
        WidgetSize.WIDE -> 168.dp
        WidgetSize.TALL -> 256.dp
    }

    val shape = RoundedCornerShape(widget.cornerRadiusDp.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_widget_${widget.appWidgetId}"),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = (glassOpacity * 0.85f).coerceAtLeast(0.22f)),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEditMode) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            widget.gifPath?.let { gif ->
                AnimatedGifMedia(
                    filePath = gif,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .graphicsLayer { alpha = 0.25f }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                if (isEditMode) {
                    WidgetEditHeader(
                        size = widget.size,
                        onCustomize = onCustomize,
                        onCycleSize = onCycleSize,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onRemove = onRemove
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (providerInfo != null && widget.appWidgetId > 0) {
                    AndroidView(
                        factory = {
                            widgetHost.createHostView(widget.appWidgetId, providerInfo)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(widgetHeight)
                            .clip(RoundedCornerShape((widget.cornerRadiusDp - 6).coerceAtLeast(8).dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = widget.providerPackage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
