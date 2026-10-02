package com.inferra.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.inferra.ui.components.glass.FloatingNavigationBar
import com.inferra.ui.components.glass.NavigationGestureState
import com.inferra.ui.components.glass.NavigationScrollState
import com.inferra.ui.components.glass.rememberNavigationGestureState
import com.inferra.ui.components.glass.rememberNavigationScrollState

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    NavItem(Screen.Discovery.route, "Discover", Icons.Default.Explore),
    NavItem(Screen.Search.route, "Search", Icons.Default.Search),
    NavItem(Screen.Compare.route, "Compare", Icons.AutoMirrored.Filled.CompareArrows),
    NavItem(Screen.Hardware.route, "Hardware", Icons.Default.DeveloperBoard),
    NavItem(Screen.Downloads.route, "Downloads", Icons.Default.Download),
)

@Composable
fun GlassBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: NavigationScrollState = rememberNavigationScrollState(),
    gestureState: NavigationGestureState = rememberNavigationGestureState(),
) {
    FloatingNavigationBar(
        items = bottomNavItems,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        scrollState = scrollState,
        gestureState = gestureState,
        modifier = modifier,
    )
}
