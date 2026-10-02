package com.inferra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import java.util.Locale

@Composable
fun ModelCard(
    model: AiModel,
    modifier: Modifier = Modifier,
    compatibility: HardwareCompatibilityResult? = null,
    onClick: () -> Unit
) {
    val charSpec = remember(model) {
        if (model.isMoe) {
            "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B MoE · ${String.format(Locale.US, "%.1f", model.activeParamsBillion)}B active"
        } else if (model.contextLengthTokens >= 131072) {
            "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B · ${model.contextLengthTokens / 1024}K context"
        } else {
            "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B parameters"
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Creator
            Text(
                text = model.author,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Model Title
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: 1 Key Characteristic + Hardware fit tag (if present)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = charSpec,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline
                )

                if (compatibility != null) {
                    val (fitText, fitColor) = when (compatibility.fitGrade) {
                        FitGrade.EXCELLENT -> Pair("Fits device", FitExcellent)
                        FitGrade.BORDERLINE -> Pair("${compatibility.offloadPercentage}% offload", FitBorderline)
                        FitGrade.INSUFFICIENT -> Pair("Requires more RAM", FitInsufficient)
                        FitGrade.UNKNOWN -> Pair("Fit unverified", MaterialTheme.colorScheme.outline)
                    }
                    GlassBadge(text = fitText, color = fitColor, showDot = true)
                }
            }
        }
    }
}
