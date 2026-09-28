package com.inferra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.AiModel
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberEmerald
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.GlassBorderSubtle
import com.inferra.ui.theme.GlassFillDark
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary

@Composable
fun LineageGraphView(
    model: AiModel,
    onNavigateToModel: (String) -> Unit,
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "Lineage Tree",
                    tint = CyberViolet,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MODEL LINEAGE TREE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberViolet,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 1: Base Model Node
            val baseName = model.lineage.baseModelId ?: "${model.author}/${model.name.replace("-Instruct", "").replace("-Chat", "")}-Base"
            LineageNode(
                title = "BASE ARCHITECTURE",
                name = baseName,
                badgeText = "Base Model",
                accentColor = CyberViolet,
                onClick = { onNavigateToModel(baseName) }
            )

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "down",
                tint = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Step 2: Current Instruct / Fine-tune Node
            LineageNode(
                title = "INSTRUCTION / ALIGNMENT",
                name = model.id,
                badgeText = "Instruct Target",
                accentColor = CyberCyan,
                isCurrentNode = true,
                onClick = { }
            )

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "down",
                tint = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Step 3: Quantization & Derivatives Node
            val quantText = if (model.quantizations.isNotEmpty()) "${model.quantizations.size} GGUF Quantizations" else "FP16 Weights"
            LineageNode(
                title = "QUANTIZATION & RUNTIME ARTIFACTS",
                name = quantText,
                badgeText = "Inference Artifacts",
                accentColor = CyberEmerald,
                onClick = { }
            )
        }
    }
}

@Composable
private fun LineageNode(
    title: String,
    name: String,
    badgeText: String,
    accentColor: Color,
    isCurrentNode: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrentNode) accentColor.copy(alpha = 0.15f) else GlassFillDark)
            .border(
                width = if (isCurrentNode) 1.5.dp else 1.dp,
                color = if (isCurrentNode) accentColor else GlassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                GlassBadge(text = badgeText, color = accentColor)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
