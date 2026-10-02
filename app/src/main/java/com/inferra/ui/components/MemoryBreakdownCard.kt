package com.inferra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.inferra.domain.model.QuantizationInfo
import com.inferra.domain.usecase.MemoryEstimationEngine
import java.util.Locale

@Composable
fun MemoryBreakdownCard(
    model: AiModel,
    quantization: QuantizationInfo?,
    modifier: Modifier = Modifier
) {
    val breakdown = MemoryEstimationEngine.estimate(model, quantization)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ARCHITECTURE & MEMORY ESTIMATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${breakdown.attentionArchitecture.name} Attention • ${String.format(Locale.US, "%.1f", breakdown.totalMemoryGb)} GB Total Memory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment memory bar
            val total = breakdown.totalMemoryMb.toFloat().coerceAtLeast(1f)
            val weightsRatio = breakdown.weightMemoryMb / total
            val kvRatio = breakdown.kvCacheMemoryMb / total
            val overheadRatio = (breakdown.activationMemoryMb + breakdown.backendOverheadMb) / total

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(weightsRatio.coerceAtLeast(0.01f))
                        .fillMaxWidth()
                        .background(Color(0xFF3B82F6)) // Blue for weights
                )
                Spacer(
                    modifier = Modifier
                        .weight(kvRatio.coerceAtLeast(0.01f))
                        .fillMaxWidth()
                        .background(Color(0xFF10B981)) // Green for KV Cache
                )
                Spacer(
                    modifier = Modifier
                        .weight(overheadRatio.coerceAtLeast(0.01f))
                        .fillMaxWidth()
                        .background(Color(0xFFF59E0B)) // Amber for Activations/Backend
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Rows
            MemoryRow(label = "Model Weights (${quantization?.quantType ?: "FP16"})", value = "${breakdown.weightMemoryMb} MB", color = Color(0xFF3B82F6))
            MemoryRow(label = "KV Cache (${model.contextLengthTokens / 1024}K Context)", value = "${breakdown.kvCacheMemoryMb} MB", color = Color(0xFF10B981))
            MemoryRow(label = "Activations & Backend Overhead", value = "${breakdown.activationMemoryMb + breakdown.backendOverheadMb} MB", color = Color(0xFFF59E0B))

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Confidence: ${breakdown.confidence.name} • ${breakdown.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemoryRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
