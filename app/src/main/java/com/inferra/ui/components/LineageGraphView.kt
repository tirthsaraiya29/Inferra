package com.inferra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferra.domain.model.AiModel

@Composable
fun LineageGraphView(
    model: AiModel,
    onNavigateToModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val baseName = remember(model) {
        model.lineage.baseModelId ?: "${model.author}/${model.name.replace("-Instruct", "").replace("-Chat", "")}-Base"
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
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
                    tint = accent,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Model Lineage",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Base Model Node
            LineageNode(
                title = "Base Model",
                name = baseName,
                badgeText = "Base",
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onNavigateToModel(baseName) }
            )

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "down",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Step 2: Current Instruct / Fine-tune Node
            LineageNode(
                title = "Aligned Variant",
                name = model.id,
                badgeText = "Current",
                accentColor = accent,
                isCurrentNode = true,
                onClick = { }
            )

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "down",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Step 3: Quantization Node
            val quantText = remember(model) {
                if (model.quantizations.isNotEmpty()) "${model.quantizations.size} GGUF Quantizations" else "FP16 Weights"
            }
            LineageNode(
                title = "Inference Formats",
                name = quantText,
                badgeText = "Quantized",
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentNode) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isCurrentNode) 1.dp else 0.5.dp,
            color = if (isCurrentNode) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = accentColor
                )
                GlassBadge(text = badgeText, color = accentColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
