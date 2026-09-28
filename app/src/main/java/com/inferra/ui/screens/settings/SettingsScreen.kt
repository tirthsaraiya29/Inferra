package com.inferra.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.inferra.ui.components.GlassBadge
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberEmerald
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsState()

    LiquidGlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APP SETTINGS & PREFERENCES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Configure visual liquid glass intensity, theme, privacy & companion settings.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Visual & Liquid Glass Engine
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Palette, contentDescription = "Glass", tint = CyberCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "LIQUID GLASS RENDER ENGINE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(text = "Glass Surface Refraction Intensity", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = state.glassIntensity,
                                onValueChange = viewModel::updateGlassIntensity,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyberCyan,
                                    activeTrackColor = CyberCyan,
                                    inactiveTrackColor = TextMuted
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Dark Cyber-Obsidian Theme", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(text = "Primary high-contrast dark visual design", fontSize = 12.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = state.isDarkMode,
                                    onCheckedChange = { viewModel.toggleDarkMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberCyan,
                                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Section: Privacy & Telemetry
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = "Privacy", tint = CyberEmerald)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "PRIVACY & OPT-IN BENCHMARKING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Opt-in Community Hardware Benchmark Contribution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(text = "Submit anonymous tokens/sec measurements to train Inferra speed predictor.", fontSize = 11.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = state.isTelemetryEnabled,
                                    onCheckedChange = { viewModel.toggleTelemetry() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberEmerald,
                                        checkedTrackColor = CyberEmerald.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Section: About
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = "About", tint = CyberViolet)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "ABOUT INFERRA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Inferra v1.0.0 • Open-weight AI Model Intelligence Platform",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Built for discovery, hardware compatibility analysis, lineage exploration, and remote desktop download management.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
