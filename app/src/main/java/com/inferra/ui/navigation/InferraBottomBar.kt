package com.inferra.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var rowWidthPx by remember { mutableIntStateOf(1) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    val scaleAnim = remember { Animatable(1.0f) }

    val activeRoute = if (hoveredIndex in bottomNavItems.indices) bottomNavItems[hoveredIndex].route else currentRoute

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scaleAnim.value)
                .onGloballyPositioned { rowWidthPx = it.size.width.coerceAtLeast(1) }
                .pointerInput(bottomNavItems.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            scope.launch {
                                scaleAnim.animateTo(1.02f, spring(stiffness = Spring.StiffnessMedium))
                            }
                            val itemWidth = rowWidthPx.toFloat() / bottomNavItems.size
                            val idx = (offset.x / itemWidth).toInt().coerceIn(0, bottomNavItems.size - 1)
                            hoveredIndex = idx
                        },
                        onDragEnd = {
                            if (hoveredIndex in bottomNavItems.indices) {
                                onNavigate(bottomNavItems[hoveredIndex].route)
                            }
                            hoveredIndex = -1
                            scope.launch {
                                scaleAnim.animateTo(1.0f, spring(stiffness = Spring.StiffnessLow))
                            }
                        },
                        onDragCancel = {
                            hoveredIndex = -1
                            scope.launch {
                                scaleAnim.animateTo(1.0f, spring(stiffness = Spring.StiffnessLow))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val itemWidth = rowWidthPx.toFloat() / bottomNavItems.size
                            val idx = (change.position.x / itemWidth).toInt().coerceIn(0, bottomNavItems.size - 1)
                            hoveredIndex = idx
                        }
                    )
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = activeRoute == item.route

                    Surface(
                        onClick = { onNavigate(item.route) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(20.dp)
                            )
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
