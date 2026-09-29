package com.inferra.ui.screens.discovery

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.GlassChip
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.components.ModelCard
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onNavigateToModel: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToHardware: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LiquidGlassBackground {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentAzure, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Discovering AI models...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (state.errorMessage != null && state.spotlightModel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to load models from Hugging Face",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage ?: "Please check your network connection and try again.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GlassButton(onClick = { viewModel.refresh() }) {
                        Text(text = "Retry", fontSize = 14.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 110.dp)
            ) {
                // Editorial Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Discover models",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Explore open-weight artificial intelligence",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Search Trigger
                        Box(modifier = Modifier.clickable { onNavigateToSearch("") }) {
                            GlassTextField(
                                value = "",
                                onValueChange = { query -> onNavigateToSearch(query) },
                                placeholderText = "Search models or capabilities...",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = TextMuted
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category quick tags
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { GlassChip(text = "Coding", isSelected = false, onClick = { onNavigateToSearch("coding") }) }
                            item { GlassChip(text = "Reasoning", isSelected = false, onClick = { onNavigateToSearch("reasoning") }) }
                            item { GlassChip(text = "Vision", isSelected = false, onClick = { onNavigateToSearch("vision") }) }
                            item { GlassChip(text = "Small models", isSelected = false, onClick = { onNavigateToSearch("small") }) }
                            item { GlassChip(text = "MoE", isSelected = false, onClick = { onNavigateToSearch("MoE") }) }
                            item { GlassChip(text = "Long context", isSelected = false, onClick = { onNavigateToSearch("context") }) }
                        }
                    }
                }

                // Featured Model
                state.spotlightModel?.let { spotlight ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = "Featured",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ModelCard(
                                model = spotlight,
                                compatibility = state.compatibilityMap[spotlight.id],
                                onClick = { onNavigateToModel(spotlight.id) }
                            )
                        }
                    }
                }

                // Trending Models
                if (state.trendingModels.isNotEmpty()) {
                    item {
                        EditorialModelSection(
                            title = "Trending models",
                            models = state.trendingModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel
                        )
                    }
                }

                // Fresh Models
                if (state.newModels.isNotEmpty()) {
                    item {
                        EditorialModelSection(
                            title = "Recently released",
                            models = state.newModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel
                        )
                    }
                }

                // Popular Models
                if (state.popularModels.isNotEmpty()) {
                    item {
                        EditorialModelSection(
                            title = "Popular",
                            models = state.popularModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialModelSection(
    title: String,
    models: List<AiModel>,
    compatibilityMap: Map<String, HardwareCompatibilityResult>,
    onModelClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(models, key = { it.id }) { model ->
                Box(modifier = Modifier.width(280.dp)) {
                    ModelCard(
                        model = model,
                        compatibility = compatibilityMap[model.id],
                        onClick = { onModelClick(model.id) }
                    )
                }
            }
        }
    }
}
