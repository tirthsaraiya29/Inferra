package com.inferra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberEmerald
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.GlassBorderSubtle
import com.inferra.ui.theme.GlassFillDark
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun CapabilityRadar(
    capabilities: CapabilityMatrix,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = GlassFillDark,
        borderColor = GlassBorderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "CAPABILITY MATRIX",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            SkillBar("Coding & Synthesis", capabilities.coding, CyberCyan)
            SkillBar("Mathematical Proofs", capabilities.math, CyberViolet)
            SkillBar("Complex Reasoning", capabilities.reasoning, CyberEmerald)
            SkillBar("Agentic Workflows", capabilities.agentic, CyberCyan)
            SkillBar("Tool & Function Calling", capabilities.toolCalling, CyberViolet)
            if (capabilities.vision > 0f) {
                SkillBar("Vision & Spatial Perception", capabilities.vision, CyberEmerald)
            }
            SkillBar("Long Context Retention", capabilities.longContext, CyberCyan)
        }
    }
}

@Composable
private fun SkillBar(
    label: String,
    score: Float,
    accentColor: Color
) {
    val progressAnim by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "skillAnim"
    )

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = String.format(Locale.US, "%.1f", score),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GlassBorderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressAnim)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f),
                                accentColor
                            )
                        )
                    )
            )
        }
    }
}
