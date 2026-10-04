package com.inferra.ui.navigation

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val mainNavItems = listOf(
    NavItem(Screen.Discovery.route, "Discover", Icons.Default.Explore),
    NavItem(Screen.Datasets.route, "Datasets", Icons.Default.Dataset),
    NavItem(Screen.Compare.route, "Compare", Icons.AutoMirrored.Filled.CompareArrows),
    NavItem(Screen.Hardware.route, "Hardware", Icons.Default.DeveloperBoard),
    NavItem(Screen.Downloads.route, "Downloads", Icons.Default.Download),
)

val searchNavItem = NavItem(Screen.Search.route, "Search", Icons.Default.Search)

val bottomNavItems = listOf(
    mainNavItems[0], // Discover
    searchNavItem,   // Search
    mainNavItems[1], // Datasets
    mainNavItems[2], // Compare
    mainNavItems[3], // Hardware
    mainNavItems[4], // Downloads
)

@Composable
fun InferraBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var isPressedOrDragging by remember { mutableStateOf(false) }

    val effectiveRoute = if (hoveredIndex != -1) {
        if (hoveredIndex == 5) searchNavItem.route
        else if (hoveredIndex in mainNavItems.indices) mainNavItems[hoveredIndex].route
        else currentRoute
    } else {
        currentRoute
    }

    val isSearchSelected = effectiveRoute == searchNavItem.route

    val activeMainIndex = remember(effectiveRoute) {
        if (isSearchSelected) {
            -1
        } else {
            val idx = mainNavItems.indexOfFirst { it.route == effectiveRoute }
            if (idx >= 0) idx else 0
        }
    }

    // ------------------------------------------------------------------
    // Slimy Jelly Scale Physics - Squish on Touch & Stretch on Move
    // ------------------------------------------------------------------
    val barScaleX by animateFloatAsState(
        targetValue = if (isPressedOrDragging) 1.050f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.45f,
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

    // Dynamic layout bounds mapping for item touch/drag gesture handling
    val tabOffsets = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ==================================================================
            // 1. MAIN FLOATING LIQUID GLASS CAPSULE BAR
            // Holds Discover, Datasets, Compare, Hardware, Downloads
            // ==================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        this.scaleX = barScaleX
                        this.scaleY = barScaleY
                    }
                    .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
            ) {
                // LAYER 1: Frosted Background Substrate with Internal Blur & Glass Refraction
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = RenderEffect
                                    .createBlurEffect(32f, 32f, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.2.dp,
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color(0x4400E5FF),
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                            shape = CircleShape
                        )
                ) {
                    // Top Specular Refraction Highlight Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.60f),
                                        Color(0x8800E5FF),
                                        Color.White.copy(alpha = 0.60f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // LAYER 2: Crisp Interactive Foreground with Slimy Jelly Gestures
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                        .pointerInput(mainNavItems.size) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val totalWidth = size.width.toFloat().coerceAtLeast(1f)

                                val calcIndex = { x: Float ->
                                    var foundIndex = -1
                                    for (i in mainNavItems.indices) {
                                        val startX = tabOffsets[i] ?: (i * (totalWidth / mainNavItems.size))
                                        val width = tabWidths[i] ?: (totalWidth / mainNavItems.size)
                                        if (x >= startX && x <= startX + width) {
                                            foundIndex = i
                                            break
                                        }
                                    }
                                    if (foundIndex != -1) foundIndex else (x / (totalWidth / mainNavItems.size)).toInt().coerceIn(0, mainNavItems.size - 1)
                                }

                                var currentIndex = calcIndex(down.position.x)
                                hoveredIndex = currentIndex
                                isPressedOrDragging = true

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()

                                    if (pointer == null || !pointer.pressed) {
                                        if (hoveredIndex in mainNavItems.indices) {
                                            onNavigate(mainNavItems[hoveredIndex].route)
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
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mainNavItems.forEachIndexed { index, item ->
                        val isSelected = activeMainIndex == index

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.18f else 0.92f,
                            animationSpec = spring(
                                dampingRatio = 0.50f,
                                stiffness = 260f
                            ),
                            label = "iconScale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .onGloballyPositioned { coords ->
                                    tabOffsets[index] = coords.positionInParent().x
                                    tabWidths[index] = coords.size.width.toFloat()
                                }
                                .graphicsLayer {
                                    if (isSelected) {
                                        this.scaleX = pillScaleX
                                        this.scaleY = pillScaleY
                                    }
                                }
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                        )
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    } else Modifier
                                )
                                .padding(horizontal = if (isSelected) 14.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .graphicsLayer {
                                        this.scaleX = iconScale
                                        this.scaleY = iconScale
                                    }
                                    .size(20.dp)
                            )

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================================================================
            // 2. STANDALONE CIRCULAR SEARCH GLASS TAB (Apple Floating Style)
            // ==================================================================
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        this.scaleX = barScaleX
                        this.scaleY = barScaleY
                    }
                    .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressedOrDragging = true
                            hoveredIndex = 5

                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()

                                if (pointer == null || !pointer.pressed) {
                                    onNavigate(searchNavItem.route)
                                    break
                                }
                            }

                            isPressedOrDragging = false
                            hoveredIndex = -1
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // LAYER 1: Frosted Background Substrate with Internal Blur & Glass Refraction
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = RenderEffect
                                    .createBlurEffect(32f, 32f, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.2.dp,
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color(0x4400E5FF),
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                            shape = CircleShape
                        )
                ) {
                    // Top Specular Refraction Highlight Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.60f),
                                        Color(0x8800E5FF),
                                        Color.White.copy(alpha = 0.60f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // LAYER 2: Active Selection Pill for Standalone Search Button
                if (isSearchSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .graphicsLayer {
                                this.scaleX = pillScaleX
                                this.scaleY = pillScaleY
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        )
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                val searchIconScale by animateFloatAsState(
                    targetValue = if (isSearchSelected) 1.22f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = 0.50f,
                        stiffness = 260f
                    ),
                    label = "searchIconScale"
                )

                Icon(
                    imageVector = searchNavItem.icon,
                    contentDescription = searchNavItem.label,
                    tint = if (isSearchSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .graphicsLayer {
                            this.scaleX = searchIconScale
                            this.scaleY = searchIconScale
                        }
                        .size(22.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun InferraBottomBarPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F0F12)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                InferraBottomBar(
                    currentRoute = Screen.Discovery.route,
                    onNavigate = {}
                )
            }
        }
    }
}
