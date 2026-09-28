package com.inferra.ui.screens.detail

import android.content.Intent
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.QuantizationInfo
import com.inferra.ui.components.CapabilityRadar
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.LineageGraphView
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.CyberAmber
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberEmerald
import com.inferra.ui.theme.CyberRose
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.GlassBorderSubtle
import com.inferra.ui.theme.GlassFillDark
import com.inferra.ui.theme.ObsidianBg
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDetailScreen(
    viewModel: ModelDetailViewModel,
    onBack: () -> Unit,
    onNavigateToModel: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDevicePickerSheet by remember { mutableStateOf(false) }

    LiquidGlassBackground {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else if (state.model == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.errorMessage ?: "Model not found.", color = TextSecondary)
            }
        } else {
            val model = state.model!!

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Top Action Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }

                        Row {
                            IconButton(onClick = viewModel::toggleWatchlist) {
                                Icon(
                                    imageVector = if (state.isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (state.isWatchlisted) CyberCyan else TextMuted
                                )
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, model.repoUrl.toUri())
                                context.startActivity(intent)
                            }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "HuggingFace Repo", tint = TextPrimary)
                            }
                        }
                    }
                }

                // Header Info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = model.author, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassBadge(text = model.licenseName, color = CyberViolet)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = model.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = model.description,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Core Specs Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SpecBox(
                                title = "TOTAL PARAMS",
                                value = "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B",
                                modifier = Modifier.weight(1f)
                            )
                            SpecBox(
                                title = "ACTIVE PARAMS",
                                value = "${String.format(Locale.US, "%.1f", model.activeParamsBillion)}B",
                                modifier = Modifier.weight(1f)
                            )
                            SpecBox(
                                title = "CONTEXT",
                                value = "${model.contextLengthTokens / 1024}K",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Hardware Compatibility & Speed Estimate Card
                state.compatibilityResult?.let { comp ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            val (gradeText, gradeColor) = when (comp.fitGrade) {
                                FitGrade.EXCELLENT -> Pair("✓ EXCELLENT FIT", FitExcellent)
                                FitGrade.BORDERLINE -> Pair("⚠ BORDERLINE (RAM Offload Required)", FitBorderline)
                                FitGrade.INSUFFICIENT -> Pair("✕ INSUFFICIENT VRAM", FitInsufficient)
                                FitGrade.UNKNOWN -> Pair("? UNKNOWN FIT", TextMuted)
                            }

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                borderColor = gradeColor.copy(alpha = 0.6f)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RUNABILITY & HARDWARE FIT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = gradeColor,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        GlassBadge(text = gradeText, color = gradeColor)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = comp.explanation,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        SpeedMetric(
                                            label = "EST. SPEED",
                                            value = "${String.format(Locale.US, "%.1f", comp.estimatedTokensPerSec)} tok/s",
                                            accentColor = CyberCyan
                                        )
                                        SpeedMetric(
                                            label = "TIME TO FIRST TOKEN",
                                            value = "${comp.estimatedTtftMs.toInt()} ms",
                                            accentColor = CyberViolet
                                        )
                                        SpeedMetric(
                                            label = "GPU OFFLOAD",
                                            value = "${comp.offloadPercentage}%",
                                            accentColor = CyberEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Capability Radar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        CapabilityRadar(capabilities = model.capabilities)
                    }
                }

                // Quantization Explorer
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "QUANTIZATIONS & DOWNLOAD ARTIFACTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        model.quantizations.forEach { quant ->
                            val isSelected = state.selectedQuantization?.id == quant.id
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { viewModel.selectQuantization(quant) },
                                borderColor = if (isSelected) CyberCyan else GlassBorderSubtle
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = quant.quantType,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CyberCyan else TextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            GlassBadge(text = quant.format, color = CyberViolet)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${formatBytes(quant.fileSizeBytes)} • Quality: ${quant.relativeQualityScore}% • ~${quant.estimatedRamMb / 1024}GB RAM",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    GlassButton(
                                        onClick = {
                                            viewModel.selectQuantization(quant)
                                            showDevicePickerSheet = true
                                        },
                                        accentColor = CyberCyan
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = "Send", modifier = Modifier.height(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Send to PC", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Lineage Tree
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        LineageGraphView(
                            model = model,
                            onNavigateToModel = onNavigateToModel
                        )
                    }
                }

                // Benchmarks Section
                if (model.benchmarks.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "BENCHMARK PROVENANCE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    model.benchmarks.forEach { bench ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = bench.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Text(text = "${bench.category} • ${bench.provenance}", fontSize = 11.sp, color = TextMuted)
                                            }
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", bench.score)}%",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberEmerald,
                                                fontFamily = FontFamily.Monospace
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

        // Send to PC Bottom Sheet Modal
        if (showDevicePickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDevicePickerSheet = false },
                containerColor = ObsidianBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "DISPATCH DOWNLOAD TO PC",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select a paired workstation or laptop to receive the download manifest.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.companionDevices.isEmpty()) {
                        Text(text = "No paired companion devices found.", color = TextMuted, fontSize = 13.sp)
                    } else {
                        state.companionDevices.forEach { dev ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                onClick = {
                                    viewModel.sendToPc(dev)
                                    showDevicePickerSheet = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Computer, contentDescription = "PC", tint = CyberCyan)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = dev.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = "${dev.ipAddress} • ${dev.osName}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SpecBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextPrimary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SpeedMetric(
    label: String,
    value: String,
    accentColor: Color
) {
    Column {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor, fontFamily = FontFamily.Monospace)
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024f * 1024f * 1024f)
    return String.format(Locale.US, "%.1f GB", gb)
}
