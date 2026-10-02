package com.inferra.ui.components.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.onClick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.ui.navigation.NavItem
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.GlassBorder
import com.inferra.ui.theme.InkCard
import com.inferra.ui.theme.TextMuted
import kotlin.math.abs

@Composable
fun FloatingNavigationBar(
    items: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: NavigationScrollState = rememberNavigationScrollState(),
    gestureState: NavigationGestureState = rememberNavigationGestureState(),
) {
    val capabilityState by rememberLiquidGlassCapability()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val selectedIdx = remember(currentRoute, items) {
        items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    LaunchedEffect(selectedIdx) {
        gestureState.selectedIndex = selectedIdx
        gestureState.candidateIndex = selectedIdx
    }

    val minimizedFraction by scrollState.animatableMinimized.asState()

    // Dynamic dimensions
    val animatedHeightDp = (64f - (16f * minimizedFraction)).dp
    val navBarPaddingBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val itemWidthPx = if (items.isNotEmpty() && barWidthPx > 0f) barWidthPx / items.size else 1f

    // Keep gesture state's selected index updated when in IDLE phase
    LaunchedEffect(selectedIdx, itemWidthPx) {
        if (gestureState.phase == NavigationGesturePhase.IDLE && itemWidthPx > 1f) {
            gestureState.updateSelectedIndex(selectedIdx, scope, itemWidthPx)
        }
    }

    // Determine optical surface tint based on active theme
    val surfaceColor = MaterialTheme.colorScheme.surface
    val themeTintRed = surfaceColor.red
    val themeTintGreen = surfaceColor.green
    val themeTintBlue = surfaceColor.blue
    val themeTintAlpha = 0.75f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (20f + 16f * minimizedFraction).dp)
            .padding(bottom = navBarPaddingBottom)
            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
    ) {
        // Floating Glass Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeightDp)
                .clip(RoundedCornerShape(32.dp))
                .then(
                    when (capabilityState.tier) {
                        LiquidGlassTier.FULL -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val runtimeShader = remember { LiquidGlassRuntimeShader() }
                                Modifier.graphicsLayer {
                                    renderEffect = runtimeShader.createComposeRenderEffect(
                                        widthPx = size.width,
                                        heightPx = size.height,
                                        cornerRadiusPx = 32.dp.toPx(),
                                        themeTintRed = themeTintRed,
                                        themeTintGreen = themeTintGreen,
                                        themeTintBlue = themeTintBlue,
                                        themeTintAlpha = themeTintAlpha,
                                        refraction = 0.06f,
                                        specular = 0.35f,
                                        rimThicknessPx = 14f,
                                        blurRadiusPx = 12f
                                    )
                                }
                            } else {
                                Modifier.background(InkCard.copy(alpha = 0.85f))
                            }
                        }
                        LiquidGlassTier.REDUCED -> Modifier.background(InkCard.copy(alpha = 0.88f))
                        LiquidGlassTier.BASIC -> Modifier.background(InkCard.copy(alpha = 0.96f))
                    }
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(32.dp)
                )
        )
        
        // Interactive Content Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeightDp)
                .clip(RoundedCornerShape(32.dp))
                .directManipulationGesture(
                    gestureState = gestureState,
                    scrollState = scrollState,
                    itemWidthPx = itemWidthPx,
                    itemCount = items.size,
                    scope = scope,
                    onNavigateToIndex = { idx ->
                        if (idx in items.indices) {
                            onNavigate(items[idx].route)
                        }
                    },
                    isReducedMotion = capabilityState.isReducedMotionEnabled
                )
        ) {
            // SINGLE CONTINUOUS LIQUID SELECTION CAPSULE
            if (barWidthPx > 0f && items.isNotEmpty()) {
                val capsuleTargetCenterX = if (gestureState.phase == NavigationGesturePhase.IDLE) {
                    selectedIdx * itemWidthPx + itemWidthPx * 0.5f
                } else {
                    gestureState.capsuleCenterX.value
                }

                val rawCapsuleWidthPx = itemWidthPx * 0.82f * gestureState.capsuleStretchRatio.value
                val capsuleWidthDp = with(density) { rawCapsuleWidthPx.toDp() }
                val capsuleHeightDp = (38f - 10f * minimizedFraction).dp * gestureState.capsuleSquishY.value
                val capsuleLeftPx = capsuleTargetCenterX - rawCapsuleWidthPx * 0.5f

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { translationX = capsuleLeftPx }
                        .width(capsuleWidthDp)
                        .height(capsuleHeightDp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentAzure.copy(alpha = 0.22f))
                        .border(0.8.dp, AccentAzure.copy(alpha = 0.50f), RoundedCornerShape(20.dp))
                )
            }

            // Interactive Tab Items Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { idx, item ->
                    val isSelected = selectedIdx == idx
                    val isCandidate = gestureState.candidateIndex == idx

                    // Proximity highlight interpolation based on continuous liquid capsule center X
                    val itemCenterX = idx * itemWidthPx + itemWidthPx * 0.5f
                    val currentCapsuleX = if (gestureState.phase == NavigationGesturePhase.IDLE) {
                        selectedIdx * itemWidthPx + itemWidthPx * 0.5f
                    } else {
                        gestureState.capsuleCenterX.value
                    }

                    val distanceFromCapsule = abs(currentCapsuleX - itemCenterX)
                    val proximityFactor = (1.0f - (distanceFromCapsule / itemWidthPx)).coerceIn(0f, 1f)

                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isSelected || isCandidate) 1.0f else (0.55f + 0.35f * proximityFactor),
                        animationSpec = spring(),
                        label = "tabAlpha"
                    )

                    val tint = if (isSelected || isCandidate || proximityFactor > 0.5f) AccentAzure else TextMuted

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                                contentDescription = item.label
                                onClick {
                                    gestureState.selectedIndex = idx
                                    gestureState.candidateIndex = idx
                                    gestureState.updateSelectedIndex(idx, scope, itemWidthPx)
                                    onNavigate(item.route)
                                    true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer { alpha = itemAlpha }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.height((20f - 2f * minimizedFraction).dp)
                            )

                            // Labels collapse smoothly on downward scroll
                            if (minimizedFraction < 0.6f) {
                                val labelAlpha = (1.0f - (minimizedFraction / 0.6f)).coerceIn(0f, 1f)
                                Spacer(modifier = Modifier.height((2f * labelAlpha).dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected || isCandidate) FontWeight.SemiBold else FontWeight.Normal,
                                    color = tint,
                                    modifier = Modifier.graphicsLayer { alpha = labelAlpha }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
