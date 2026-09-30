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
import androidx.compose.runtime.setValue
import kotlin.math.abs
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
    initialSelectedIndex: Int = 0,
) {
    var phase by mutableStateOf(NavigationGesturePhase.IDLE)
    var selectedIndex by mutableIntStateOf(initialSelectedIndex)
    var candidateIndex by mutableIntStateOf(initialSelectedIndex)

    // Continuous liquid capsule center X along the bar
    val capsuleCenterX: Animatable<Float, AnimationVector1D> = Animatable(0f)

    // Slime liquid stretch ratio (1.0f = normal pill, 1.35f = elongated during drag)
    val capsuleStretchRatio: Animatable<Float, AnimationVector1D> = Animatable(1.0f)

    // Slime liquid squish Y (1.0f = normal height, 0.88f = compressed)
    val capsuleSquishY: Animatable<Float, AnimationVector1D> = Animatable(1.0f)

    val mutatorMutex = MutatorMutex()

    fun updateSelectedIndex(index: Int, scope: CoroutineScope, itemWidthPx: Float) {
        if (selectedIndex != index && phase == NavigationGesturePhase.IDLE) {
            selectedIndex = index
            candidateIndex = index
            scope.launch {
                mutatorMutex.mutate {
                    capsuleCenterX.snapTo(index * itemWidthPx + itemWidthPx * 0.5f)
                    capsuleStretchRatio.snapTo(1.0f)
                    capsuleSquishY.snapTo(1.0f)
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
                capsuleCenterX.snapTo(initialX)
                capsuleSquishY.animateTo(
                    0.88f,
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
        scope: CoroutineScope,
    ) {
        phase = NavigationGesturePhase.DRAGGING

        val totalWidthPx = itemCount * itemWidthPx
        val newCenterX = (currentX + dragAmountX).coerceIn(itemWidthPx * 0.5f, totalWidthPx - itemWidthPx * 0.5f)

        // Dynamically compute candidate index based on touch center position
        val newCandidate = (newCenterX / itemWidthPx).toInt().coerceIn(0, itemCount - 1)
        candidateIndex = newCandidate

        // Liquid stretch deformation based on drag speed/distance
        val targetStretch = (1.0f + (abs(dragAmountX) / 12f).coerceIn(0f, 0.35f))

        scope.launch {
            mutatorMutex.mutate {
                capsuleCenterX.snapTo(newCenterX)
                capsuleStretchRatio.snapTo(targetStretch)
                capsuleSquishY.snapTo(0.92f)
            }
        }
    }

    fun onRelease(
        itemWidthPx: Float,
        onNavigateToIndex: (Int) -> Unit,
        scope: CoroutineScope,
        isReducedMotion: Boolean = false,
    ) {
        val targetIdx = candidateIndex
        val targetCenterX = targetIdx * itemWidthPx + itemWidthPx * 0.5f

        phase = NavigationGesturePhase.COMMITTING

        scope.launch {
            mutatorMutex.mutate {
                if (isReducedMotion) {
                    capsuleCenterX.snapTo(targetCenterX)
                    capsuleStretchRatio.snapTo(1.0f)
                    capsuleSquishY.snapTo(1.0f)
                } else {
                    // Viscous liquid spring settling animation
                    launch {
                        capsuleStretchRatio.animateTo(
                            1.0f,
                            spring(
                                stiffness = Spring.StiffnessMedium,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        )
                    }
                    launch {
                        capsuleSquishY.animateTo(
                            1.0f,
                            spring(
                                stiffness = Spring.StiffnessMedium,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        )
                    }
                    capsuleCenterX.animateTo(
                        targetCenterX,
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
        val originalCenterX = selectedIndex * itemWidthPx + itemWidthPx * 0.5f
        phase = NavigationGesturePhase.SPRING_BACK
        candidateIndex = selectedIndex

        scope.launch {
            mutatorMutex.mutate {
                launch { capsuleStretchRatio.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium)) }
                launch { capsuleSquishY.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium)) }
                capsuleCenterX.animateTo(originalCenterX, spring(stiffness = Spring.StiffnessLow))
                phase = NavigationGesturePhase.IDLE
            }
        }
    }
}

@Composable
fun rememberNavigationGestureState(
    initialSelectedIndex: Int = 0,
): NavigationGestureState {
    return remember { NavigationGestureState(initialSelectedIndex) }
}
