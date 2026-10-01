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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.inferra.domain.model.EvidenceStrength
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.QualityEvidence
import com.inferra.ui.components.BenchmarkComparisonChart
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.components.MemoryBreakdownCard
import com.inferra.ui.components.ProviderComparisonView
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
    onNavigateToModel: (String) -> Unit = {},
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to load model detail",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage ?: "Model details could not be retrieved from Hugging Face.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GlassButton(onClick = { viewModel.retry() }) {
                        Text(text = "Retry", fontSize = 14.sp)
                    }
                }
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
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, model.repoUrl.toUri())
                                    context.startActivity(intent)
                                }
                            ) {
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

                // Level 2: Architecture & Memory Breakdown Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        MemoryBreakdownCard(
                            model = model,
                            quantization = state.selectedQuantization
                        )
                    }
                }

                // Level 3: Canonical Benchmark Evidence Chart
                if (state.benchmarks.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            BenchmarkComparisonChart(benchmarks = state.benchmarks)
                        }
                    }
                }

                // Level 4: Provider Pricing & Performance
                if (state.providerRows.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            ProviderComparisonView(items = state.providerRows)
                        }
                    }
                }

                // Level 5: Hardware & Memory Requirements Analysis
                state.compatibilityResult?.let { comp ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            val (gradeText, gradeColor) = when (comp.fitGrade) {
                                FitGrade.EXCELLENT -> Pair("Fits workstation RAM/VRAM", FitExcellent)
                                FitGrade.BORDERLINE -> Pair("Offload required (${comp.offloadPercentage}%)", FitBorderline)
                                FitGrade.INSUFFICIENT -> Pair("High memory requirement", FitInsufficient)
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
                                            text = "Hardware Requirement Analysis",
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
                                }
                            }
                        }
                    }
                }

                // Level 6: Active Artifact Download Progress
                state.activeLocalDownloadJob?.let { job ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = AccentAzure
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Downloading artifact: ${job.manifest.fileName}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            val downloadedMb = job.downloadedBytes / (1024f * 1024f)
                                            val totalMb = job.totalBytes / (1024f * 1024f)
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", downloadedMb)} MB / ${String.format(Locale.US, "%.1f", totalMb)} MB • ${formatSpeed(job.speedBytesPerSec)}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        IconButton(onClick = { viewModel.cancelLocalDownload(job.id) }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = TextMuted)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val progress = if (job.totalBytes > 0) (job.downloadedBytes.toFloat() / job.totalBytes).coerceIn(0f, 1f) else 0f
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = AccentAzure
                                    )
                                }
                            }
                        }
                    }
                }

                // Level 7: Discovered Quantizations & Model Artifacts
                if (model.quantizations.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Discovered Model Artifacts (${model.quantizations.size})",
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
                                        .padding(vertical = 6.dp),
                                    onClick = { viewModel.selectQuantization(quant) },
                                    borderColor = if (isSelected) AccentAzure else GlassBorder
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = quant.quantType,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) AccentAzure else TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    GlassBadge(text = quant.format, color = TextMuted)
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "File: ${quant.fileName}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "${formatBytes(quant.fileSizeBytes)} • Est. Weight RAM: ~${quant.estimatedRamMb / 1024} GB",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                GlassButton(
                                                    onClick = {
                                                        viewModel.selectQuantization(quant)
                                                        viewModel.downloadToDevice(quant)
                                                    },
                                                    accentColor = AccentAzure
                                                ) {
                                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.height(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "Save Artifact", fontSize = 12.sp)
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                GlassButton(
                                                    onClick = {
                                                        viewModel.selectQuantization(quant)
                                                        showDevicePickerSheet = true
                                                    }
                                                ) {
                                                    Text(text = "Export to Workstation", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        QualityEvidenceView(evidence = quant.qualityEvidence)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export to Workstation Bottom Sheet Modal
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
                        text = "Export Artifact Manifest to Workstation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a paired workstation or server to receive the model download manifest.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.companionDevices.isEmpty()) {
                        Text(text = "No paired workstations found.", color = TextMuted, fontSize = 13.sp)
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
private fun QualityEvidenceView(evidence: QualityEvidence) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = InkCard,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model Quality Retention Evidence",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val (badgeLabel, badgeColor) = when (evidence.strength) {
                    EvidenceStrength.STRONG -> Pair("Strong Evidence", FitExcellent)
                    EvidenceStrength.MODERATE -> Pair("Moderate Evidence", AccentAzure)
                    EvidenceStrength.LIMITED -> Pair("Limited Evidence", FitBorderline)
                    EvidenceStrength.INSUFFICIENT -> Pair("Insufficient Evidence", TextMuted)
                }

                GlassBadge(text = badgeLabel, color = badgeColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if ((evidence.strength == EvidenceStrength.INSUFFICIENT) || evidence.retentions.isEmpty()) {
                Text(
                    text = "Insufficient quality evidence found for this quantization on Hugging Face.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else {
                evidence.retentions.forEach { ret ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ret.benchmarkName} retention:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", ret.retentionPercentage)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ret.retentionPercentage >= 95f) FitExcellent else if (ret.retentionPercentage >= 85f) FitBorderline else FitInsufficient
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val gb = bytes / (1024f * 1024f * 1024f)
    return String.format(Locale.US, "%.1f GB", gb)
}

private fun formatSpeed(bytesPerSec: Long): String {
    val mbPerSec = bytesPerSec / (1024f * 1024f)
    return String.format(Locale.US, "%.1f MB/s", mbPerSec)
}
