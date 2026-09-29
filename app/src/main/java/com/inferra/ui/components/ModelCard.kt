package com.inferra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.GlassBorder
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ModelCard(
    model: AiModel,
    modifier: Modifier = Modifier,
    compatibility: HardwareCompatibilityResult? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = GlassBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Creator
            Text(
                text = model.author,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AccentAzure
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Model Title
            Text(
                text = model.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = model.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: 1 Key Characteristic + Hardware fit tag (if present)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary characteristic summary
                val charSpec = if (model.isMoe) {
                    "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B MoE · ${String.format(Locale.US, "%.1f", model.activeParamsBillion)}B active"
                } else if (model.contextLengthTokens >= 131072) {
                    "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B · ${model.contextLengthTokens / 1024}K context"
                } else {
                    "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B parameters"
                }

                Text(
                    text = charSpec,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )

                if (compatibility != null) {
                    val (fitText, fitColor) = when (compatibility.fitGrade) {
                        FitGrade.EXCELLENT -> Pair("Fits device", FitExcellent)
                        FitGrade.BORDERLINE -> Pair("${compatibility.offloadPercentage}% offload", FitBorderline)
                        FitGrade.INSUFFICIENT -> Pair("Requires more RAM", FitInsufficient)
                        FitGrade.UNKNOWN -> Pair("Fit unverified", TextMuted)
                    }
                    GlassBadge(text = fitText, color = fitColor, showDot = true)
                }
            }
        }
    }
}
