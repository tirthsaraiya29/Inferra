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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.AiModel
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.GlassBorder
import com.inferra.ui.theme.InkCard
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary

@Composable
fun LineageGraphView(
    model: AiModel,
    onNavigateToModel: (String) -> Unit,
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
                    tint = AccentAzure,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Model Lineage",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Base Model Node
            val baseName = model.lineage.baseModelId ?: "${model.author}/${model.name.replace("-Instruct", "").replace("-Chat", "")}-Base"
            LineageNode(
                title = "Base Model",
                name = baseName,
                badgeText = "Base",
                accentColor = TextMuted,
            ) { onNavigateToModel(baseName) }

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "down",
                tint = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Step 2: Current Instruct / Fine-tune Node
            LineageNode(
                title = "Aligned Variant",
                name = model.id,
                badgeText = "Current",
                accentColor = AccentAzure,
                isCurrentNode = true,
                onClick = { }
            )

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "down",
                tint = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Step 3: Quantization Node
            val quantText = if (model.quantizations.isNotEmpty()) "${model.quantizations.size} GGUF Quantizations" else "FP16 Weights"
            LineageNode(
                title = "Inference Formats",
                name = quantText,
                badgeText = "Quantized",
                accentColor = TextMuted,
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentNode) accentColor.copy(alpha = 0.12f) else InkCard)
            .border(
                width = if (isCurrentNode) 1.dp else 0.5.dp,
                color = if (isCurrentNode) accentColor else GlassBorder,
                shape = RoundedCornerShape(12.dp)
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
                GlassBadge(text = badgeText, color = accentColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}
