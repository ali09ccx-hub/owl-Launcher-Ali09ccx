package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.model.GestureId
import kotlin.math.abs

/**
 * Multi-touch & edge gesture recognizer supporting 24 distinct gestures plus Swipe-Up-and-Hold for Recents.
 *
 * Uses a two-stage pointer pipeline:
 * 1) Initial pass consumes multi-touch (>= 2 fingers) so HorizontalPager/LazyColumn do not scroll
 *    during 2/3/4-finger pinches, rotations, or swipes.
 * 2) Final pass inspects raw finger travel regardless of LazyColumn consumption so 1-finger
 *    Swipe Up reliably opens the App Drawer and Swipe-Up-and-Hold opens Recents without breaking scrolling or drag-and-drop.
 */
@Composable
fun OwlGestureOverlay(
    onGesture: (GestureId) -> Unit,
    onLongPressEmptyArea: () -> Unit,
    onSwipeUpAndHold: (() -> Unit)? = null,
    isChildDragging: Boolean = false,
    canScrollBackward: () -> Boolean = { false },
    canScrollForward: () -> Boolean = { false },
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val edgeThresholdPx = with(density) { 26.dp.toPx() }
    val swipeThresholdPx = with(density) { 44.dp.toPx() }
    val tapSlopPx = with(density) { 18.dp.toPx() }
    val longPressTimeoutMs = 520L
    val swipeUpHoldDelayMs = 230L
    val multiTapWindowMs = 280L

    val latestOnGesture by rememberUpdatedState(onGesture)
    val latestOnLongPressEmpty by rememberUpdatedState(onLongPressEmptyArea)
    val latestOnSwipeUpAndHold by rememberUpdatedState(onSwipeUpAndHold)
    val latestChildDragging by rememberUpdatedState(isChildDragging)
    val latestCanScrollBackward by rememberUpdatedState(canScrollBackward)
    val latestCanScrollForward by rememberUpdatedState(canScrollForward)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Stage 1: Protect multi-touch gestures from being stolen by HorizontalPager / LazyColumn
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2 && !latestChildDragging) {
                            event.changes.forEach {
                                if (it.positionChanged()) it.consume()
                            }
                        }
                        if (pressed == 0) break
                    }
                }
            }
            // Stage 2: Observe complete gesture lifecycle in Final pass
            .pointerInput(Unit) {
                var oneFingerTapCount = 0
                var lastOneFingerTapTime = 0L
                var twoFingerTapCount = 0
                var lastTwoFingerTapTime = 0L

                awaitEachGesture {
                    val firstDown = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Final
                    )
                    val downTime = System.currentTimeMillis()
                    val startPos = firstDown.position
                    val trackedId = firstDown.id
                    val widthPx = size.width.toFloat()
                    val heightPx = size.height.toFloat()

                    var maxPointers = 1
                    var totalPan = Offset.Zero
                    var cumulativeZoom = 1f
                    var cumulativeRotation = 0f
                    var maxTravel = 0f
                    var longPressTriggered = false
                    var gestureEmitted = false
                    var anyChildConsumedHold = firstDown.isConsumed
                    var abortedByChildDrag = latestChildDragging
                    var upwardHoldReadySince = 0L

                    while (true) {
                        val allowSwipeUpHold = maxPointers == 1 &&
                            !abortedByChildDrag &&
                            !gestureEmitted &&
                            !longPressTriggered &&
                            latestOnSwipeUpAndHold != null &&
                            totalPan.y < -swipeThresholdPx &&
                            abs(totalPan.y) > abs(totalPan.x) * 1.15f &&
                            (!latestCanScrollForward() || startPos.y > heightPx * 0.48f)

                        val nowBeforeWait = System.currentTimeMillis()
                        if (allowSwipeUpHold) {
                            if (upwardHoldReadySince == 0L) upwardHoldReadySince = nowBeforeWait
                        } else {
                            upwardHoldReadySince = 0L
                        }

                        val event = if (allowSwipeUpHold) {
                            val elapsedHold = nowBeforeWait - upwardHoldReadySince
                            val remainingHold = (swipeUpHoldDelayMs - elapsedHold).coerceAtLeast(1L)
                            withTimeoutOrNull(remainingHold) {
                                awaitPointerEvent(pass = PointerEventPass.Final)
                            }
                        } else {
                            awaitPointerEvent(pass = PointerEventPass.Final)
                        }

                        if (event == null) {
                            // Finger stayed held steady after swiping upward -> open Recents!
                            if (!latestChildDragging && !gestureEmitted) {
                                gestureEmitted = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                latestOnSwipeUpAndHold?.invoke()
                            }
                            continue
                        }

                        if (latestChildDragging) {
                            abortedByChildDrag = true
                        }
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount > maxPointers) maxPointers = pressedCount

                        val elapsed = System.currentTimeMillis() - downTime
                        if (event.changes.any { it.isConsumed } && maxTravel < tapSlopPx) {
                            anyChildConsumedHold = true
                        }

                        // Empty-area long press opens Home Control Bar (Settings & Edit Pencil)
                        if (maxPointers == 1 && !abortedByChildDrag &&
                            !longPressTriggered && !gestureEmitted &&
                            !anyChildConsumedHold &&
                            elapsed >= longPressTimeoutMs && maxTravel < tapSlopPx
                        ) {
                            longPressTriggered = true
                            latestOnLongPressEmpty()
                        }

                        if (pressedCount >= 2 && !abortedByChildDrag) {
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            totalPan += pan
                            cumulativeZoom *= zoom
                            cumulativeRotation += rotation
                            maxTravel = maxOf(maxTravel, totalPan.getDistance())

                            if (!gestureEmitted) {
                                when {
                                    maxPointers == 2 && cumulativeZoom < 0.78f -> {
                                        gestureEmitted = true
                                        latestOnGesture(GestureId.TWO_F_PINCH_IN)
                                    }
                                    maxPointers == 2 && cumulativeZoom > 1.28f -> {
                                        gestureEmitted = true
                                        latestOnGesture(GestureId.TWO_F_SPREAD_OUT)
                                    }
                                    maxPointers == 2 && cumulativeRotation > 26f -> {
                                        gestureEmitted = true
                                        latestOnGesture(GestureId.TWO_F_ROTATE_CW)
                                    }
                                    maxPointers == 2 && cumulativeRotation < -26f -> {
                                        gestureEmitted = true
                                        latestOnGesture(GestureId.TWO_F_ROTATE_CCW)
                                    }
                                }
                            }
                        } else if (pressedCount == 1 && !abortedByChildDrag) {
                            val change = event.changes.firstOrNull { it.id == trackedId }
                                ?: event.changes.firstOrNull()
                            if (change != null) {
                                val delta = change.position - change.previousPosition
                                totalPan += delta
                                maxTravel = maxOf(maxTravel, totalPan.getDistance())
                                if (upwardHoldReadySince > 0L && delta.getDistance() > 8f) {
                                    // Reset hold timer while still actively swiping upward
                                    upwardHoldReadySince = System.currentTimeMillis()
                                }

                                // Detect Edge Swipe immediately
                                if (!gestureEmitted && maxPointers == 1) {
                                    if (startPos.x <= edgeThresholdPx &&
                                        totalPan.x > swipeThresholdPx &&
                                        abs(totalPan.x) > abs(totalPan.y) * 1.4f
                                    ) {
                                        gestureEmitted = true
                                        latestOnGesture(GestureId.EDGE_LEFT_IN)
                                    } else if (startPos.x >= widthPx - edgeThresholdPx &&
                                        totalPan.x < -swipeThresholdPx &&
                                        abs(totalPan.x) > abs(totalPan.y) * 1.4f
                                    ) {
                                        gestureEmitted = true
                                        latestOnGesture(GestureId.EDGE_RIGHT_IN)
                                    }
                                }
                            }
                        }

                        if (pressedCount == 0) {
                            break
                        }
                    }

                    if (!gestureEmitted && !longPressTriggered && !abortedByChildDrag) {
                        val now = System.currentTimeMillis()
                        val gestureDurationMs = now - downTime
                        val absX = abs(totalPan.x)
                        val absY = abs(totalPan.y)

                        when (maxPointers) {
                            1 -> {
                                if (absY > swipeThresholdPx && absY > absX * 1.15f) {
                                    if (totalPan.y < 0f) {
                                        // 1-Finger Swipe Up -> Reliably open All Apps Drawer
                                        val allowSwipeUp = !latestCanScrollForward() ||
                                            startPos.y > heightPx * 0.48f ||
                                            gestureDurationMs < 650L
                                        if (allowSwipeUp) {
                                            latestOnGesture(GestureId.ONE_F_SWIPE_UP)
                                        }
                                    } else {
                                        val allowSwipeDown = !latestCanScrollBackward() ||
                                            startPos.y < heightPx * 0.25f ||
                                            gestureDurationMs < 420L
                                        if (allowSwipeDown) {
                                            latestOnGesture(GestureId.ONE_F_SWIPE_DOWN)
                                        }
                                    }
                                } else if (maxTravel < tapSlopPx && !anyChildConsumedHold) {
                                    if (now - lastOneFingerTapTime <= multiTapWindowMs) {
                                        oneFingerTapCount++
                                    } else {
                                        oneFingerTapCount = 1
                                    }
                                    lastOneFingerTapTime = now
                                    if (oneFingerTapCount == 2) {
                                        latestOnGesture(GestureId.ONE_F_DOUBLE_TAP)
                                    } else if (oneFingerTapCount >= 3) {
                                        oneFingerTapCount = 0
                                        latestOnGesture(GestureId.ONE_F_TRIPLE_TAP)
                                    }
                                }
                            }
                            2 -> {
                                if (maxTravel < tapSlopPx) {
                                    if (now - lastTwoFingerTapTime <= multiTapWindowMs) {
                                        twoFingerTapCount++
                                    } else {
                                        twoFingerTapCount = 1
                                    }
                                    lastTwoFingerTapTime = now
                                    if (twoFingerTapCount >= 2) {
                                        twoFingerTapCount = 0
                                        latestOnGesture(GestureId.TWO_F_DOUBLE_TAP)
                                    } else {
                                        latestOnGesture(GestureId.TWO_F_TAP)
                                    }
                                } else if (absY > swipeThresholdPx || absX > swipeThresholdPx) {
                                    if (absY >= absX) {
                                        latestOnGesture(
                                            if (totalPan.y < 0f) GestureId.TWO_F_SWIPE_UP
                                            else GestureId.TWO_F_SWIPE_DOWN
                                        )
                                    } else {
                                        latestOnGesture(
                                            if (totalPan.x < 0f) GestureId.TWO_F_SWIPE_LEFT
                                            else GestureId.TWO_F_SWIPE_RIGHT
                                        )
                                    }
                                }
                            }
                            3 -> {
                                if (maxTravel < tapSlopPx) {
                                    latestOnGesture(GestureId.THREE_F_TAP)
                                } else if (absY > swipeThresholdPx || absX > swipeThresholdPx) {
                                    if (absY >= absX) {
                                        latestOnGesture(
                                            if (totalPan.y < 0f) GestureId.THREE_F_SWIPE_UP
                                            else GestureId.THREE_F_SWIPE_DOWN
                                        )
                                    } else {
                                        latestOnGesture(
                                            if (totalPan.x < 0f) GestureId.THREE_F_SWIPE_LEFT
                                            else GestureId.THREE_F_SWIPE_RIGHT
                                        )
                                    }
                                }
                            }
                            else -> {
                                if (maxTravel < tapSlopPx) {
                                    latestOnGesture(GestureId.FOUR_F_TAP)
                                } else if (absY > swipeThresholdPx) {
                                    latestOnGesture(
                                        if (totalPan.y < 0f) GestureId.FOUR_F_SWIPE_UP
                                        else GestureId.FOUR_F_SWIPE_DOWN
                                    )
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
