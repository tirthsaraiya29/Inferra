package com.inferra.ui.components.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
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

@Composable
fun FloatingNavigationBar(
    items: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    scrollState: NavigationScrollState = rememberNavigationScrollState(),
    gestureState: NavigationGestureState = rememberNavigationGestureState(),
    modifier: Modifier = Modifier
) {
    val capabilityState by rememberLiquidGlassCapability()
    val scope = rememberCoroutineScope()

    val selectedIdx = remember(currentRoute, items) {
        items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    LaunchedEffect(selectedIdx) {
        gestureState.selectedIndex = selectedIdx
        gestureState.candidateIndex = selectedIdx
    }

    val minimizedFraction by scrollState.animatableMinimized.asState()

    // Dynamic dimensions
    val animatedHeightDp = (64f - 16f * minimizedFraction).dp
    val navBarPaddingBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val itemWidthPx = if (items.isNotEmpty() && barWidthPx > 0f) barWidthPx / items.size else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (20f + 16f * minimizedFraction).dp)
            .padding(bottom = navBarPaddingBottom)
            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
    ) {
        // Floating Glass Surface Container
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
                                        refraction = 0.05f,
                                        specular = 0.35f,
                                        rimThicknessPx = 16f,
                                        blurRadiusPx = 28f
                                    )
                                }
                            } else {
                                Modifier.background(InkCard.copy(alpha = 0.88f))
                            }
                        }
                        LiquidGlassTier.REDUCED -> Modifier.background(InkCard.copy(alpha = 0.90f))
                        LiquidGlassTier.BASIC -> Modifier.background(InkCard.copy(alpha = 0.96f))
                    }
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(32.dp)
                )
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

                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isSelected || isCandidate) 1.0f else 0.65f,
                        animationSpec = spring(),
                        label = "tabAlpha"
                    )

                    val tint = if (isSelected || isCandidate) AccentAzure else TextMuted

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                                contentDescription = item.label
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                gestureState.selectedIndex = idx
                                gestureState.candidateIndex = idx
                                onNavigate(item.route)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Capsule highlight under active/candidate item
                        if (isSelected || isCandidate) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height((38f - 10f * minimizedFraction).dp)
                                    .scale(if (gestureState.phase == NavigationGesturePhase.DRAGGING) gestureState.capsuleScaleX.value else 1.0f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AccentAzure.copy(alpha = if (isSelected) 0.20f else 0.10f))
                                    .border(0.5.dp, AccentAzure.copy(alpha = 0.40f), RoundedCornerShape(20.dp))
                            )
                        }

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
                                    fontSize = (10f * labelAlpha).sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
