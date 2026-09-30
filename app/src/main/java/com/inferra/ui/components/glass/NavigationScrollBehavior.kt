package com.inferra.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class NavigationScrollState(
    initialMinimized: Float = 0.0f,
) {
    var isGestureActive by mutableStateOf(value = false)

    // Current expansion/minimized fraction: 0.0f = Expanded, 1.0f = Minimized
    val animatableMinimized = Animatable(initialMinimized)

    var accumulatedDelta by mutableFloatStateOf(0.0f)
    private val hysteresisThresholdPx = 36.0f // ~16.dp hysteresis threshold to prevent scroll jitter

    fun onScroll(consumedDy: Float, scope: CoroutineScope) {
        if (isGestureActive) return // Suspend scroll minimization while direct manipulation gesture is active

        accumulatedDelta += consumedDy

        if (accumulatedDelta < -hysteresisThresholdPx) {
            // Sustained downward scroll -> Minimize bar
            scope.launch {
                animatableMinimized.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            }
            accumulatedDelta = 0.0f
        } else if (accumulatedDelta > hysteresisThresholdPx) {
            // Sustained upward scroll -> Expand bar
            scope.launch {
                animatableMinimized.animateTo(
                    targetValue = 0.0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            }
            accumulatedDelta = 0.0f
        }
    }

    fun forceExpand(scope: CoroutineScope) {
        accumulatedDelta = 0.0f
        scope.launch {
            animatableMinimized.animateTo(
                targetValue = 0.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        }
    }
}

class NavigationNestedScrollConnection(
    val scrollState: NavigationScrollState,
    val scope: CoroutineScope,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Delta dy: negative means scrolling down (content moves up), positive means scrolling up
        if (source == NestedScrollSource.UserInput) {
            scrollState.onScroll(available.y, scope)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.UserInput) {
            scrollState.onScroll(consumed.y, scope)
        }
        return Offset.Zero
    }
}

@Composable
fun rememberNavigationScrollState(): NavigationScrollState {
    return remember { NavigationScrollState() }
}

@Composable
fun rememberNavigationNestedScrollConnection(
    scrollState: NavigationScrollState = rememberNavigationScrollState(),
): NavigationNestedScrollConnection {
    val scope = rememberCoroutineScope()
    return remember(scrollState, scope) {
        NavigationNestedScrollConnection(scrollState, scope)
    }
}
