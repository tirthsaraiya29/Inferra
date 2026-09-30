package com.inferra.ui.screens.compare

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.FitGrade
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.GlassChip
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CompareScreen(
    viewModel: CompareViewModel,
    onNavigateToModel: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LiquidGlassBackground {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentAzure, strokeWidth = 2.dp)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Compare", tint = AccentAzure)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compare Models",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Select models to analyze parameters, specs, and hardware fit side-by-side",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.availableModels, key = { it.id }) { model ->
                            val isSelected = state.selectedModels.any { it.id == model.id }
                            GlassChip(
                                text = model.name,
                                isSelected = isSelected,
                                onClick = { viewModel.selectModel(model) },
                                accentColor = AccentAzure
                            )
                        }
                    }
                }

                if (state.selectedModels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select at least 1 model above to compare.", color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    val scrollState = rememberScrollState()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 110.dp)
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                state.selectedModels.forEach { model ->
                                    Box(modifier = Modifier.width(220.dp)) {
                                        GlassCard(onClick = { onNavigateToModel(model.id) }) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(text = model.author, fontSize = 12.sp, color = AccentAzure, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = model.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                GlassBadge(text = model.licenseName, color = TextMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            CompareSectionTitle("Parameters & Architecture")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                state.selectedModels.forEach { model ->
                                    CompareCell(
                                        label = "Total Params",
                                        value = "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B",
                                        subValue = if (model.isMoe) "(${String.format(Locale.US, "%.1f", model.activeParamsBillion)}B active)" else "Dense"
                                    )
                                }
                            }
                        }

                        item {
                            CompareSectionTitle("Context Window")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                state.selectedModels.forEach { model ->
                                    CompareCell(
                                        label = "Max Context",
                                        value = "${model.contextLengthTokens / 1024}K tokens",
                                        accentColor = AccentAzure
                                    )
                                }
                            }
                        }

                        item {
                            CompareSectionTitle("Hardware Fit (${state.activeHardwareProfile?.gpuName ?: "GPU"})")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                state.selectedModels.forEach { model ->
                                    val comp = state.compatibilityMap[model.id]
                                    val fitText = when (comp?.fitGrade) {
                                        FitGrade.EXCELLENT -> "Fits device"
                                        FitGrade.BORDERLINE -> "${comp.offloadPercentage}% Offload"
                                        FitGrade.INSUFFICIENT -> "Requires RAM"
                                        else -> "Unknown"
                                    }
                                    val fitColor = when (comp?.fitGrade) {
                                        FitGrade.EXCELLENT -> FitExcellent
                                        FitGrade.BORDERLINE -> FitBorderline
                                        FitGrade.INSUFFICIENT -> FitInsufficient
                                        else -> TextMuted
                                    }

                                    CompareCell(
                                        label = "Est. Memory Fit",
                                        value = fitText,
                                        subValue = "Inferra estimate",
                                        accentColor = fitColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun CompareCell(
    label: String,
    value: String,
    subValue: String? = null,
    accentColor: Color = TextPrimary
) {
    Box(modifier = Modifier.width(220.dp)) {
        GlassCard(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = label, fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                if (subValue != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subValue, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}
