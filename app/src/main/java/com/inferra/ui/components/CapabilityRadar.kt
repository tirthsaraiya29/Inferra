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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.CapabilityMatrix
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.GlassBorder
import com.inferra.ui.theme.InkCard
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
        shape = RoundedCornerShape(16.dp),
        backgroundColor = InkCard,
        borderColor = GlassBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Capabilities Summary",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SkillBar("Coding & Synthesis", capabilities.coding, AccentAzure)
            SkillBar("Mathematical Proofs", capabilities.math, AccentAzure)
            SkillBar("Complex Reasoning", capabilities.reasoning, AccentAzure)
            SkillBar("Agentic Workflows", capabilities.agentic, AccentAzure)
            SkillBar("Tool & Function Calling", capabilities.toolCalling, AccentAzure)
            if (capabilities.vision > 0f) {
                SkillBar("Vision & Perception", capabilities.vision, AccentAzure)
            }
            SkillBar("Long Context Retention", capabilities.longContext, AccentAzure)
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
                text = String.format(Locale.US, "%.0f%%", score),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(GlassBorder)
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
