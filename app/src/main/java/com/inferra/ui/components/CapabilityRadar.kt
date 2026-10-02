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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferra.domain.model.CapabilityMatrix
import java.util.Locale

@Composable
fun CapabilityRadar(
    capabilities: CapabilityMatrix,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Capabilities Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            SkillBar("Coding & Synthesis", capabilities.coding, accent)
            SkillBar("Mathematical Proofs", capabilities.math, accent)
            SkillBar("Complex Reasoning", capabilities.reasoning, accent)
            SkillBar("Agentic Workflows", capabilities.agentic, accent)
            SkillBar("Tool & Function Calling", capabilities.toolCalling, accent)
            if (capabilities.vision > 0f) {
                SkillBar("Vision & Perception", capabilities.vision, accent)
            }
            SkillBar("Long Context Retention", capabilities.longContext, accent)
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(Locale.US, "%.0f%%", score),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = (score / 100f).coerceIn(0f, 1f),
                        range = 0f..1f
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressAnim)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )
        }
    }
}
