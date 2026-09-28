package com.inferra.ui.screens.discovery

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.GlassChip
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.components.ModelCard
import com.inferra.ui.theme.CyberAmber
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberEmerald
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.GlassBorderSubtle
import com.inferra.ui.theme.GlassFillDark
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onNavigateToModel: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToHardware: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LiquidGlassBackground {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Analyzing Model Intelligence...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "INFERRA",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "AI Model Intelligence & Discovery",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            state.activeHardwareProfile?.let { profile ->
                                GlassCard(
                                    onClick = onNavigateToHardware,
                                    shape = RoundedCornerShape(30.dp),
                                    backgroundColor = GlassFillDark,
                                    borderColor = GlassBorderSubtle
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Computer,
                                            contentDescription = "Hardware",
                                            tint = CyberCyan,
                                            modifier = Modifier.height(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${profile.gpuName} (${profile.vramGb.toInt()}GB)",
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        GlassTextField(
                            value = "",
                            onValueChange = { query -> onNavigateToSearch(query) },
                            placeholderText = "Search models (e.g. 'coding', '32B', 'GGUF', 'Qwen')...",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = CyberCyan
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { GlassChip(text = "⚡ Coding", isSelected = false, onClick = { onNavigateToSearch("coding") }) }
                            item { GlassChip(text = "👁 Vision", isSelected = false, onClick = { onNavigateToSearch("vision") }) }
                            item { GlassChip(text = "📦 GGUF", isSelected = false, onClick = { onNavigateToSearch("GGUF") }) }
                            item { GlassChip(text = "🔥 <8GB VRAM", isSelected = false, onClick = { onNavigateToSearch("8B") }) }
                            item { GlassChip(text = "🧠 MoE", isSelected = false, onClick = { onNavigateToSearch("MoE") }) }
                        }
                    }
                }

                state.spotlightModel?.let { spotlight ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            SectionHeader(
                                title = "FEATURED MODEL SPOTLIGHT",
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ModelCard(
                                model = spotlight,
                                compatibility = state.compatibilityMap[spotlight.id],
                                onClick = { onNavigateToModel(spotlight.id) }
                            )
                        }
                    }
                }

                if (state.forYouModels.isNotEmpty()) {
                    item {
                        ModelSectionCarousel(
                            title = "FOR YOUR HARDWARE",
                            subtitle = "Models optimized for ${state.activeHardwareProfile?.gpuName ?: "Your System"}",
                            models = state.forYouModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel,
                            accentColor = CyberEmerald
                        )
                    }
                }

                if (state.trendingModels.isNotEmpty()) {
                    item {
                        ModelSectionCarousel(
                            title = "TRENDING INTELLIGENCE",
                            subtitle = "Rapidly gaining community adoption & downloads",
                            models = state.trendingModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel,
                            accentColor = CyberAmber
                        )
                    }
                }

                if (state.newModels.isNotEmpty()) {
                    item {
                        ModelSectionCarousel(
                            title = "FRESH MODEL DROPS",
                            subtitle = "Recently published open-weight releases",
                            models = state.newModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel,
                            accentColor = CyberViolet
                        )
                    }
                }

                if (state.popularModels.isNotEmpty()) {
                    item {
                        ModelSectionCarousel(
                            title = "POPULAR WORKHORSES",
                            subtitle = "Top downloaded foundational models",
                            models = state.popularModels,
                            compatibilityMap = state.compatibilityMap,
                            onModelClick = onNavigateToModel,
                            accentColor = CyberCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: Color
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

@Composable
private fun ModelSectionCarousel(
    title: String,
    subtitle: String,
    models: List<AiModel>,
    compatibilityMap: Map<String, HardwareCompatibilityResult>,
    onModelClick: (String) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionHeader(title = title, color = accentColor)
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(models, key = { it.id }) { model ->
                Box(modifier = Modifier.width(300.dp)) {
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
