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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.ui.components.GlassCard
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.FitExcellent
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
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = AccentAzure)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "App preferences and rendering controls",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Visual Material Engine
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Palette, contentDescription = "Glass", tint = AccentAzure)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Glass Material Intensity", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(text = "Material Refraction Level", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = state.glassIntensity,
                                onValueChange = viewModel::updateGlassIntensity,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentAzure,
                                    activeTrackColor = AccentAzure,
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
                                    Text(text = "Editorial Dark Theme", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Text(text = "Quiet high-contrast dark theme", fontSize = 12.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = state.isDarkMode,
                                    onCheckedChange = { viewModel.toggleDarkMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AccentAzure,
                                        checkedTrackColor = AccentAzure.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Section: Hugging Face User Access Token (Gated Models)
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = "Token", tint = AccentAzure)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Hugging Face Access Token", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Enter your user access token (hf_...) to discover gated or private models like Llama 3.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            GlassTextField(
                                value = state.hfToken,
                                onValueChange = viewModel::updateHfToken,
                                placeholderText = "hf_..."
                            )
                        }
                    }
                }

                // Section: Privacy & Diagnostics
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = "Privacy", tint = FitExcellent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Privacy & Diagnostics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Anonymous speed estimation feedback", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Text(text = "Contribute tokens/sec measurements to improve Inferra's hardware fit model.", fontSize = 11.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = state.isTelemetryEnabled,
                                    onCheckedChange = { viewModel.toggleTelemetry() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = FitExcellent,
                                        checkedTrackColor = FitExcellent.copy(alpha = 0.3f)
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
                                Icon(imageVector = Icons.Default.Info, contentDescription = "About", tint = AccentAzure)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "About Inferra", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Inferra v1.0.0",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A quiet, intelligent application for discovering and understanding AI models.",
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
