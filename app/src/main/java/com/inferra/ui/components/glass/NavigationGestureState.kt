package com.inferra.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class NavigationGesturePhase {
    IDLE,
    PRESSED,
    DRAGGING,
    COMMITTING,
    SPRING_BACK
}

@Stable
class NavigationGestureState(
    initialSelectedIndex: Int = 0
) {
    var phase by mutableStateOf(NavigationGesturePhase.IDLE)
    var selectedIndex by mutableIntStateOf(initialSelectedIndex)
    var candidateIndex by mutableIntStateOf(initialSelectedIndex)

    // Animated capsule center position X along the bar
    val capsuleOffsetX: Animatable<Float, AnimationVector1D> = Animatable(0f)

    // Animated capsule scale/press compression
    val capsuleScaleX: Animatable<Float, AnimationVector1D> = Animatable(1.0f)

    val mutatorMutex = MutatorMutex()

    fun updateSelectedIndex(index: Int, scope: CoroutineScope, itemWidthPx: Float) {
        if (selectedIndex != index && phase == NavigationGesturePhase.IDLE) {
            selectedIndex = index
            candidateIndex = index
            scope.launch {
                mutatorMutex.mutate {
                    capsuleOffsetX.snapTo(index * itemWidthPx)
                }
            }
        }
    }

    fun onPress(selectedIdx: Int, initialX: Float, scope: CoroutineScope) {
        phase = NavigationGesturePhase.PRESSED
        selectedIndex = selectedIdx
        candidateIndex = selectedIdx

        scope.launch {
            mutatorMutex.mutate {
                capsuleOffsetX.snapTo(initialX)
                capsuleScaleX.animateTo(
                    0.92f,
                    spring(stiffness = Spring.StiffnessHigh)
                )
            }
        }
    }

    fun onDrag(
        dragAmountX: Float,
        currentX: Float,
        itemWidthPx: Float,
        itemCount: Int,
        scope: CoroutineScope
    ) {
        phase = NavigationGesturePhase.DRAGGING

        val newCapsuleX = (currentX + dragAmountX).coerceIn(0f, (itemCount - 1) * itemWidthPx)

        // Calculate new candidate index based on touch position
        val newCandidate = (newCapsuleX / itemWidthPx + 0.5f).toInt().coerceIn(0, itemCount - 1)
        candidateIndex = newCandidate

        scope.launch {
            mutatorMutex.mutate {
                capsuleOffsetX.snapTo(newCapsuleX)
                capsuleScaleX.snapTo(1.08f) // Subtle stretch during drag
            }
        }
    }

    fun onRelease(
        itemWidthPx: Float,
        onNavigateToIndex: (Int) -> Unit,
        scope: CoroutineScope,
        isReducedMotion: Boolean = false
    ) {
        val targetIdx = candidateIndex
        val targetX = targetIdx * itemWidthPx

        phase = NavigationGesturePhase.COMMITTING

        scope.launch {
            mutatorMutex.mutate {
                if (isReducedMotion) {
                    capsuleOffsetX.snapTo(targetX)
                    capsuleScaleX.snapTo(1.0f)
                } else {
                    capsuleScaleX.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium))
                    capsuleOffsetX.animateTo(
                        targetX,
                        spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                }

                selectedIndex = targetIdx
                phase = NavigationGesturePhase.IDLE
                onNavigateToIndex(targetIdx)
            }
        }
    }

    fun onCancel(itemWidthPx: Float, scope: CoroutineScope) {
        val originalX = selectedIndex * itemWidthPx
        phase = NavigationGesturePhase.SPRING_BACK
        candidateIndex = selectedIndex

        scope.launch {
            mutatorMutex.mutate {
                capsuleScaleX.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium))
                capsuleOffsetX.animateTo(originalX, spring(stiffness = Spring.StiffnessLow))
                phase = NavigationGesturePhase.IDLE
            }
        }
    }
}

@Composable
fun rememberNavigationGestureState(
    initialSelectedIndex: Int = 0
): NavigationGestureState {
    return remember { NavigationGestureState(initialSelectedIndex) }
}
