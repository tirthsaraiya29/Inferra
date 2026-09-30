package com.inferra.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lan
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.DownloadStatus
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.InkBg
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showPairDialog by remember { mutableStateOf(false) }

    var devName by remember { mutableStateOf("") }
    var devIp by remember { mutableStateOf("192.168.1.") }

    LiquidGlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Downloads", tint = AccentAzure)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Downloads",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monitor downloads dispatched to companion workstations",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    GlassButton(
                        onClick = { showPairDialog = true },
                        accentColor = AccentAzure
                    ) {
                        Icon(imageVector = Icons.Default.Lan, contentDescription = "Pair", modifier = Modifier.height(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Pair PC", fontSize = 12.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(text = "Active Jobs", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                if (state.jobs.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No active download jobs queued.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(state.jobs, key = { it.id }) { job ->
                        val (statusText, statusColor) = when (job.status) {
                            DownloadStatus.DOWNLOADING -> Pair("Downloading (${formatSpeed(job.speedBytesPerSec)})", AccentAzure)
                            DownloadStatus.QUEUED -> Pair("Queued", FitBorderline)
                            DownloadStatus.WAITING_FOR_DEVICE -> Pair("Waiting for PC", TextMuted)
                            DownloadStatus.COMPLETED -> Pair("Completed", FitExcellent)
                            DownloadStatus.PAUSED -> Pair("Paused", TextMuted)
                            DownloadStatus.FAILED -> Pair("Failed", FitInsufficient)
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = job.author, fontSize = 12.sp, color = AccentAzure, fontWeight = FontWeight.Medium)
                                        Text(text = job.modelName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    IconButton(onClick = { viewModel.cancelJob(job.id) }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = TextMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassBadge(text = job.quantType, color = TextMuted)
                                    GlassBadge(text = statusText, color = statusColor)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val progressPct = if (job.totalBytes > 0) (job.downloadedBytes.toFloat() / job.totalBytes.toFloat()) else 0f
                                Text(
                                    text = "Target: ${job.targetDeviceName} • Progress: ${(progressPct * 100).toInt()}% • ETA: ${job.etaSeconds}s",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Installed Local Models (${state.installedLocalModels.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                if (state.installedLocalModels.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No local model files downloaded yet.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(state.installedLocalModels, key = { it.id }) { localModel ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = localModel.modelName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = localModel.fileName, fontSize = 12.sp, color = TextSecondary)
                                    }
                                    IconButton(onClick = { viewModel.deleteLocalModel(localModel.id) }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = FitInsufficient)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassBadge(text = localModel.quantType, color = AccentAzure)
                                    val sizeGb = localModel.fileSizeBytes / (1024f * 1024f * 1024f)
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f GB", sizeGb)} • ${localModel.format}",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Paired Companions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                items(state.devices, key = { it.id }) { dev ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Computer, contentDescription = "Device", tint = AccentAzure)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = dev.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "${dev.ipAddress}:${dev.port} • ${dev.osName}", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            GlassBadge(
                                text = if (dev.isOnline) "Online" else "Offline",
                                color = if (dev.isOnline) FitExcellent else TextMuted
                            )
                        }
                    }
                }
            }
        }

        if (showPairDialog) {
            ModalBottomSheet(
                onDismissRequest = { showPairDialog = false },
                containerColor = InkBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Pair companion workstation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlassTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        placeholderText = "PC Name (e.g. 'Desktop Workstation')"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassTextField(
                        value = devIp,
                        onValueChange = { devIp = it },
                        placeholderText = "IP Address (e.g. 192.168.1.105)"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    GlassButton(
                        onClick = {
                            if (devName.isNotBlank() && devIp.isNotBlank()) {
                                viewModel.pairNewDevice(devName, devIp, 8443)
                                showPairDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pair Device", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val mb = bytesPerSec / (1024f * 1024f)
    return String.format(Locale.US, "%.1f MB/s", mb)
}
