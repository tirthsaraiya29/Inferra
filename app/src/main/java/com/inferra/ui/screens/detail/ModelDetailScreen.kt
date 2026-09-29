package com.inferra.ui.screens.detail

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.inferra.domain.model.FitGrade
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.GlassBorder
import com.inferra.ui.theme.InkBg
import com.inferra.ui.theme.InkCard
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDetailScreen(
    viewModel: ModelDetailViewModel,
    onBack: () -> Unit,
    onNavigateToModel: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDevicePickerSheet by remember { mutableStateOf(false) }

    LiquidGlassBackground {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentAzure, strokeWidth = 2.dp)
            }
        } else if (state.model == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.errorMessage ?: "Model not found.", color = TextSecondary)
            }
        } else {
            val model = state.model!!

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
            ) {
                // Top Action Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
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
                                    tint = if (state.isWatchlisted) AccentAzure else TextMuted
                                )
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, model.repoUrl.toUri())
                                context.startActivity(intent)
                            }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Repository", tint = TextPrimary)
                            }
                        }
                    }
                }

                // Level 1: What is this model?
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = model.author, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AccentAzure)
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassBadge(text = model.licenseName, color = TextMuted)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = model.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = model.description,
                            fontSize = 15.sp,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Level 2: Can I run it? (Inferra Hardware Fit Estimate)
                state.compatibilityResult?.let { comp ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            val (gradeText, gradeColor) = when (comp.fitGrade) {
                                FitGrade.EXCELLENT -> Pair("Fits active device", FitExcellent)
                                FitGrade.BORDERLINE -> Pair("Offload required (${comp.offloadPercentage}%)", FitBorderline)
                                FitGrade.INSUFFICIENT -> Pair("Requires more RAM", FitInsufficient)
                                FitGrade.UNKNOWN -> Pair("Hardware profile not configured", TextMuted)
                            }

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                borderColor = GlassBorder
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Runability Estimate",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        GlassBadge(text = gradeText, color = gradeColor, showDot = true)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = comp.explanation,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "* Calculated based on active hardware profile. Inferra estimate.",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Level 3: Technical Specifications Overview
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Specifications",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SpecBox(
                                title = "PARAMETERS",
                                value = if (model.isMoe) "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B MoE" else "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B",
                                modifier = Modifier.weight(1f)
                            )
                            SpecBox(
                                title = "CONTEXT",
                                value = "${model.contextLengthTokens / 1024}K tokens",
                                modifier = Modifier.weight(1f)
                            )
                            SpecBox(
                                title = "ARCHITECTURE",
                                value = model.architecture.take(12),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Level 4: Verified Benchmarks
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Verified Benchmarks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (model.benchmarks.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No verified benchmark data",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            model.benchmarks.forEach { bench ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = bench.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                        Text(text = bench.category, fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", bench.score)}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAzure
                                    )
                                }
                            }
                        }
                    }
                }

                // Level 5: Quantizations & Download Files
                if (model.quantizations.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Quantizations",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            model.quantizations.forEach { quant ->
                                val isSelected = state.selectedQuantization?.id == quant.id
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { viewModel.selectQuantization(quant) },
                                    borderColor = if (isSelected) AccentAzure else GlassBorder
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
                                                    color = if (isSelected) AccentAzure else TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                GlassBadge(text = quant.format, color = TextMuted)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${formatBytes(quant.fileSizeBytes)} • ~${quant.estimatedRamMb / 1024}GB RAM required",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        GlassButton(
                                            onClick = {
                                                viewModel.selectQuantization(quant)
                                                showDevicePickerSheet = true
                                            },
                                            accentColor = AccentAzure
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = "Send", modifier = Modifier.height(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Send to PC", fontSize = 12.sp)
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
                containerColor = InkBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Send download to PC",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a paired workstation to receive the download manifest.",
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
                                    Icon(imageVector = Icons.Default.Computer, contentDescription = "PC", tint = AccentAzure)
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
        shape = RoundedCornerShape(12.dp),
        backgroundColor = InkCard
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024f * 1024f * 1024f)
    return String.format(Locale.US, "%.1f GB", gb)
}
