package com.inferra.ui.components.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CoroutineScope

fun Modifier.directManipulationGesture(
    gestureState: NavigationGestureState,
    scrollState: NavigationScrollState,
    itemWidthPx: Float,
    itemCount: Int,
    scope: CoroutineScope,
    onNavigateToIndex: (Int) -> Unit,
    isReducedMotion: Boolean = false,
): Modifier = this.pointerInput(gestureState, scrollState, itemWidthPx, itemCount, isReducedMotion) {
    awaitEachGesture {
        val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
        val initialX = down.position.x
        val initialSelected = gestureState.selectedIndex

        scrollState.isGestureActive = true
        gestureState.onPress(initialSelected, initialX, scope)

        var currentX = initialX
        var isDragging = false

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            val dragEvent = event.changes.firstOrNull { it.id == down.id }

            if ((dragEvent != null) && dragEvent.pressed) {
                val dx = dragEvent.positionChange().x
                if ((dx != 0f) || isDragging) {
                    isDragging = true
                    dragEvent.consume()
                    gestureState.onDrag(
                        dragAmountX = dx,
                        currentX = currentX,
                        itemWidthPx = itemWidthPx,
                        itemCount = itemCount,
                        scope = scope,
                    )
                    currentX += dx
                }
            } else if (dragEvent != null) {
                // Pointer released
                scrollState.isGestureActive = false
                if (isDragging) {
                    dragEvent.consume()
                    gestureState.onRelease(
                        itemWidthPx = itemWidthPx,
                        onNavigateToIndex = onNavigateToIndex,
                        scope = scope,
                        isReducedMotion = isReducedMotion,
                    )
                } else {
                    // Quick tap released -> compute candidate from touch position and navigate
                    val targetIndex = (currentX / itemWidthPx).toInt().coerceIn(0, itemCount - 1)
                    gestureState.candidateIndex = targetIndex
                    dragEvent.consume()
                    gestureState.onRelease(
                        itemWidthPx = itemWidthPx,
                        onNavigateToIndex = onNavigateToIndex,
                        scope = scope,
                        isReducedMotion = isReducedMotion
                    )
                }
                break
            } else {
                // Gesture cancelled or lost
                scrollState.isGestureActive = false
                gestureState.onCancel(itemWidthPx, scope)
                break
            }
        } while (true)
    }
}
