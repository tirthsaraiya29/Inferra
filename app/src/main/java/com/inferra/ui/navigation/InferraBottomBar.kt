package com.inferra.ui.navigation

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    NavItem(Screen.Discovery.route, "Discover", Icons.Default.Explore),
    NavItem(Screen.Search.route, "Search", Icons.Default.Search),
    NavItem(Screen.Datasets.route, "Datasets", Icons.Default.Dataset),
    NavItem(Screen.Compare.route, "Compare", Icons.AutoMirrored.Filled.CompareArrows),
    NavItem(Screen.Hardware.route, "Hardware", Icons.Default.DeveloperBoard),
    NavItem(Screen.Downloads.route, "Downloads", Icons.Default.Download),
)

@Composable
fun InferraBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var innerRowWidthPx by remember { mutableFloatStateOf(1f) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var isPressedOrDragging by remember { mutableStateOf(false) }

    val effectiveRoute = if (hoveredIndex in bottomNavItems.indices) {
        bottomNavItems[hoveredIndex].route
    } else {
        currentRoute
    }

    val activeIndex = remember(effectiveRoute, currentRoute, hoveredIndex) {
        if (hoveredIndex in bottomNavItems.indices) {
            hoveredIndex
        } else {
            val idx = bottomNavItems.indexOfFirst { it.route == currentRoute }
            if (idx >= 0) idx else 0
        }
    }

    // Slimy Jelly Scale Physics - Squish on Touch & Stretch on Move
    val barScaleX by animateFloatAsState(
        targetValue = if (isPressedOrDragging) 1.050f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.45f, // Super springy slimy bounce!
            stiffness = 180f
        ),
        label = "jellyBarScaleX"
    )

    val barScaleY by animateFloatAsState(
        targetValue = if (isPressedOrDragging) 0.880f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = 180f
        ),
        label = "jellyBarScaleY"
    )

    val pillScaleX by animateFloatAsState(
        targetValue = if (isPressedOrDragging) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.50f,
            stiffness = 200f
        ),
        label = "pillScaleX"
    )

    val pillScaleY by animateFloatAsState(
        targetValue = if (isPressedOrDragging) 0.86f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.50f,
            stiffness = 200f
        ),
        label = "pillScaleY"
    )

    val density = LocalDensity.current
    val itemWidthPx = (innerRowWidthPx / bottomNavItems.size).coerceAtLeast(1f)
    val targetPillX = activeIndex * itemWidthPx

    val animatedPillX by animateFloatAsState(
        targetValue = targetPillX,
        animationSpec = spring(
            dampingRatio = 0.55f, // Liquid fluid slide
            stiffness = 180f
        ),
        label = "liquidPillX"
    )

    val itemWidthDp = with(density) { itemWidthPx.toDp() }
    val animatedPillXDp = with(density) { animatedPillX.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Refracted Liquid Glass Floating Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.scaleX = barScaleX
                    this.scaleY = barScaleY
                    renderEffect = RenderEffect
                        .createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color(0x3300E5FF), // Subtle aquatic refraction shimmer
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(4.dp)
        ) {
            // Specular Top Shine Line (Refracted Water/Glass Edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color(0x6600E5FF),
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner Row Container for Exact Alignment & Pointer Gesture Handling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .onGloballyPositioned { innerRowWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(bottomNavItems.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val totalWidth = size.width.toFloat().coerceAtLeast(1f)
                            val perItemWidth = totalWidth / bottomNavItems.size

                            val calcIndex = { x: Float ->
                                (x / perItemWidth).toInt().coerceIn(0, bottomNavItems.size - 1)
                            }

                            var currentIndex = calcIndex(down.position.x)
                            hoveredIndex = currentIndex
                            isPressedOrDragging = true

                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()

                                if (pointer == null || !pointer.pressed) {
                                    // Release event!
                                    if (hoveredIndex in bottomNavItems.indices) {
                                        onNavigate(bottomNavItems[hoveredIndex].route)
                                    }
                                    break
                                }

                                val newIndex = calcIndex(pointer.position.x)
                                if (newIndex != currentIndex) {
                                    currentIndex = newIndex
                                    hoveredIndex = newIndex
                                }
                            }

                            isPressedOrDragging = false
                            hoveredIndex = -1
                        }
                    }
            ) {
                // Liquid Refraction Radial Aura
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    val glowCenterX = animatedPillX + itemWidthPx / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(glowCenterX, size.height / 2f),
                            radius = itemWidthPx * 0.95f
                        ),
                        radius = itemWidthPx * 0.95f,
                        center = Offset(glowCenterX, size.height / 2f)
                    )
                }

                // Sliding Liquid Glass Selection Pill - 100% Perfectly Centered
                Box(
                    modifier = Modifier
                        .offset(x = animatedPillXDp)
                        .width(itemWidthDp)
                        .height(54.dp)
                        .graphicsLayer {
                            this.scaleX = pillScaleX
                            this.scaleY = pillScaleY
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                )

                // Navigation Items Row - Equal width items matching inner width
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val isSelected = activeIndex == index

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.22f else 0.92f,
                            animationSpec = spring(
                                dampingRatio = 0.50f,
                                stiffness = 260f
                            ),
                            label = "iconScale"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .graphicsLayer {
                                        this.scaleX = iconScale
                                        this.scaleY = iconScale
                                    }
                                    .height(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
