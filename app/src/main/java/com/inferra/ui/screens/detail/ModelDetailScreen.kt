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
import com.inferra.domain.model.DownloadStatus
import com.inferra.domain.model.EvidenceStrength
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.QualityEvidence
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
    onNavigateToModel: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDevicePickerSheet by remember { mutableStateOf(value = false) }

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
                                        text = "* Calculated based on active hardware profile. Inferra memory fit estimate.",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Level 3: Active Download Progress Bar (If downloading locally)
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
                                                text = "Downloading ${job.manifest.fileName}",
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

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (job.status == DownloadStatus.COMPLETED) "Download Complete!" else if (job.status == DownloadStatus.FAILED) "Failed: ${job.errorMessage}" else "ETA: ${job.etaSeconds}s",
                                        fontSize = 11.sp,
                                        color = if (job.status == DownloadStatus.FAILED) FitInsufficient else AccentAzure
                                    )
                                }
                            }
                        }
                    }
                }

                // Level 4: Technical Specifications Overview
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
                                value = if (model.totalParamsBillion <= 0f) "Not specified" else if (model.isMoe) "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B MoE" else "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B",
                                modifier = Modifier.weight(1f)
                            )
                            SpecBox(
                                title = "CONTEXT",
                                value = if (model.contextLengthTokens <= 0) "Not specified" else "${model.contextLengthTokens / 1024}K tokens",
                                modifier = Modifier.weight(1f)
                            )
                            SpecBox(
                                title = "ARCHITECTURE",
                                value = model.architecture.ifBlank { "Unknown" }.take(12),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Level 5: Dynamic Hugging Face Quantizations & Quality Evidence
                if (model.quantizations.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Discovered Quantizations (${model.quantizations.size})",
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

                                                if (quant.sourceRepo.isNotBlank()) {
                                                    Text(
                                                        text = "Repo: ${quant.sourceRepo}",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "${formatBytes(quant.fileSizeBytes)} • Estimated RAM: ~${quant.estimatedRamMb / 1024} GB",
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
                                                    Text(text = "Download", fontSize = 12.sp)
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                GlassButton(
                                                    onClick = {
                                                        viewModel.selectQuantization(quant)
                                                        showDevicePickerSheet = true
                                                    }
                                                ) {
                                                    Text(text = "Send to PC", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }
                                        }

                                        // Quality Evidence Section for this Quant
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
                    text = "Model Quality Evidence",
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

                if (evidence.sourceUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Source: ${evidence.sourceRepo} • Baseline: ${evidence.comparisonBaseline}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
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
    if (bytes <= 0L) return "Unknown size"
    val gb = bytes / (1024f * 1024f * 1024f)
    return String.format(Locale.US, "%.1f GB", gb)
}

private fun formatSpeed(bytesPerSec: Long): String {
    val mbPerSec = bytesPerSec / (1024f * 1024f)
    return String.format(Locale.US, "%.1f MB/s", mbPerSec)
}
