package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.HomeStickyText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Renders persisted text directly over the Home wallpaper (transparent cardless floating text)
 * so text typed via the 4-finger or 8-finger tap gesture stays visible, draggable, and editable.
 */
@Composable
fun HomeStickyTextLayer(
    stickyText: HomeStickyText?,
    isEditMode: Boolean,
    onMovePosition: (Float, Float) -> Unit,
    onTapToEdit: () -> Unit,
    onClearText: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (stickyText == null || stickyText.text.isBlank()) return

    var xFrac by remember(stickyText.pageIndex, stickyText.xFraction) {
        mutableFloatStateOf(stickyText.xFraction.coerceIn(0.08f, 0.92f))
    }
    var yFrac by remember(stickyText.pageIndex, stickyText.yFraction) {
        mutableFloatStateOf(stickyText.yFraction.coerceIn(0.06f, 0.85f))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val estBoxWidthPx = with(density) { 260.dp.toPx() }
            val posX = (xFrac * widthPx - estBoxWidthPx / 2f).roundToInt()
                .coerceIn(0, (widthPx - estBoxWidthPx * 0.4f).roundToInt().coerceAtLeast(0))
            val posY = (yFrac * heightPx).roundToInt()
                .coerceIn(0, (heightPx * 0.82f).roundToInt().coerceAtLeast(0))

            Box(
                modifier = Modifier
                    .offset { IntOffset(posX, posY) }
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isEditMode) {
                            Modifier.background(Color.Black.copy(alpha = 0.28f))
                        } else {
                            Modifier
                        }
                    )
                    .pointerInput(stickyText.pageIndex) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                xFrac = (xFrac + dragAmount.x / widthPx).coerceIn(0.08f, 0.92f)
                                yFrac = (yFrac + dragAmount.y / heightPx).coerceIn(0.06f, 0.85f)
                            },
                            onDragEnd = {
                                onMovePosition(xFrac, yFrac)
                            }
                        )
                    }
                    .clickable(onClick = onTapToEdit)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("home_sticky_text_${stickyText.pageIndex}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stickyText.text,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = stickyText.fontSizeSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                offset = Offset(1.5f, 2f),
                                blurRadius = 6f
                            )
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable(onClick = onClearText),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.home_typer_clear),
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Keyboard typer bar triggered by 8-finger or 4-finger tap on the Home screen.
 * Automatically opens Android IME and updates Home screen text live while typing;
 * closing keeps the typed text pinned persistently on the Home screen.
 */
@Composable
fun HomeKeyboardTyperOverlay(
    currentPage: Int,
    stickyText: HomeStickyText?,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onClearAndClose: () -> Unit,
    onDismissKeepText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentText = stickyText?.text.orEmpty()
    val currentFontSp = stickyText?.fontSizeSp ?: 22

    LaunchedEffect(Unit) {
        delay(60L)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismissKeepText)
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            tonalElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .clickable(enabled = false) {}
                .testTag("home_keyboard_typer_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.ga_open_home_typer)} • ${currentPage + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onFontSizeChange((currentFontSp - 2).coerceAtLeast(14)) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${currentFontSp}sp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { onFontSizeChange((currentFontSp + 2).coerceAtMost(42)) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = currentText,
                    onValueChange = onTextChange,
                    placeholder = { Text(stringResource(R.string.home_typer_hint)) },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onDismissKeepText()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("home_typer_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        keyboardController?.hide()
                        onClearAndClose()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_typer_clear),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onDismissKeepText()
                        },
                        modifier = Modifier.testTag("home_typer_keep_btn")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.home_typer_keep))
                    }
                }
            }
        }
    }
}
