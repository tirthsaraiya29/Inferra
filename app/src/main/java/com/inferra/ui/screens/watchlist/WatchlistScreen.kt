package com.inferra.ui.screens.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.components.ModelCard
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberRose
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextSecondary

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onNavigateToModel: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LiquidGlassBackground {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Watchlist", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAVED MODELS & WATCHLIST",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tracked open-weight models saved for offline analysis & download.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (state.savedModels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.BookmarkRemove, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.height(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Your watchlist is currently empty.", color = TextSecondary, fontSize = 14.sp)
                            Text(text = "Tap the bookmark icon on any model detail page to save it.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.savedModels, key = { it.id }) { model ->
                            ModelCard(
                                model = model,
                                compatibility = state.compatibilityMap[model.id],
                                onClick = { onNavigateToModel(model.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
