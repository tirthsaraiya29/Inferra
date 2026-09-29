package com.inferra.ui.screens.hardware

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.DeviceType
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.GlassBorder
import com.inferra.ui.theme.InkBg
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newGpu by remember { mutableStateOf("") }
    var newVram by remember { mutableStateOf("24") }
    var newRam by remember { mutableStateOf("64") }
    val newCpu by remember { mutableStateOf("x86_64 CPU") }

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
                            Icon(imageVector = Icons.Default.DeveloperBoard, contentDescription = "Hardware", tint = AccentAzure)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hardware Profiles",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure devices for local hardware runability estimates",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    GlassButton(
                        onClick = { showAddDialog = true },
                        accentColor = AccentAzure
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.height(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Device", fontSize = 12.sp)
                    }
                }
            }

            if (state.profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Hardware profile not configured",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hardware profiles configured yet. Add a workstation or detect this device's memory to calculate model runability.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassButton(onClick = { viewModel.detectAndAddLocalHardware() }) {
                                Text(text = "Detect Device Hardware", fontSize = 13.sp)
                            }
                            GlassButton(onClick = { showAddDialog = true }) {
                                Text(text = "Add Custom Profile", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.profiles, key = { it.id }) { profile ->
                        val isActive = state.activeProfile?.id == profile.id
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (isActive) AccentAzure else GlassBorder
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (profile.deviceType == DeviceType.LOCAL_ANDROID) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                                            contentDescription = "Type",
                                            tint = AccentAzure
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = profile.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    if (isActive) {
                                        GlassBadge(text = "Active", color = AccentAzure)
                                    } else {
                                        IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FitInsufficient)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SpecPill(label = "GPU", value = profile.gpuName, color = TextPrimary)
                                    SpecPill(label = "VRAM", value = "${profile.vramGb.toInt()} GB", color = AccentAzure)
                                    SpecPill(label = "SYSTEM RAM", value = "${profile.ramGb.toInt()} GB", color = TextSecondary)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "CPU: ${profile.cpuName} • OS: ${profile.osName}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = { showAddDialog = false },
                containerColor = InkBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Add hardware profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlassTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholderText = "Device Name (e.g. 'Desktop Workstation')"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassTextField(
                        value = newGpu,
                        onValueChange = { newGpu = it },
                        placeholderText = "GPU Model (e.g. 'RTX 4090')"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassTextField(
                            value = newVram,
                            onValueChange = { newVram = it },
                            placeholderText = "VRAM (GB)",
                            modifier = Modifier.weight(1f)
                        )
                        GlassTextField(
                            value = newRam,
                            onValueChange = { newRam = it },
                            placeholderText = "RAM (GB)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    GlassButton(
                        onClick = {
                            if (newName.isNotBlank() && newGpu.isNotBlank()) {
                                viewModel.addCustomPcProfile(
                                    name = newName,
                                    gpuName = newGpu,
                                    vramGb = newVram.toFloatOrNull() ?: 24f,
                                    ramGb = newRam.toFloatOrNull() ?: 64f,
                                    cpuName = newCpu
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Profile", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SpecPill(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
