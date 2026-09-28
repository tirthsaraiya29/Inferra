package com.inferra.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.GlassBorderLight
import com.inferra.ui.theme.GlassFillDark
import com.inferra.ui.theme.TextMuted

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    NavItem(Screen.Discovery.route, "Discover", Icons.Default.Explore),
    NavItem(Screen.Search.route, "Search", Icons.Default.Search),
    NavItem(Screen.Compare.route, "Compare", Icons.AutoMirrored.Filled.CompareArrows),
    NavItem(Screen.Hardware.route, "Hardware", Icons.Default.DeveloperBoard),
    NavItem(Screen.Downloads.route, "Downloads", Icons.Default.Download),
    NavItem(Screen.Watchlist.route, "Saved", Icons.Default.Bookmark),
    NavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
)

@Composable
fun GlassBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(GlassFillDark)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GlassBorderLight.copy(alpha = 0.6f),
                            GlassBorderLight.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val tint = if (isSelected) CyberCyan else TextMuted

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(item.route) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.height(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
