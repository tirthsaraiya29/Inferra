package com.inferra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.AiModel
import com.inferra.domain.model.FitGrade
import com.inferra.domain.model.HardwareCompatibilityResult
import com.inferra.domain.model.Modality
import com.inferra.ui.theme.CyberAmber
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberEmerald
import com.inferra.ui.theme.CyberRose
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.FitBorderline
import com.inferra.ui.theme.FitExcellent
import com.inferra.ui.theme.FitInsufficient
import com.inferra.ui.theme.GlassBorderSubtle
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ModelCard(
    model: AiModel,
    compatibility: HardwareCompatibilityResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (model.isFeatured) CyberCyan.copy(alpha = 0.4f) else GlassBorderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Author + Badges + Downloads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.author,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyberCyan
                    )
                    if (model.isTrending) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassBadge(text = "🔥 Trending", color = CyberAmber)
                    }
                    if (model.isMoe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GlassBadge(text = "MoE", color = CyberViolet)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = TextMuted,
                        modifier = Modifier.height(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(model.downloadsCount),
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Model Title
            Text(
                text = model.name,
                fontSize = 17.sp,
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
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Parameter count + Active
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Params",
                        tint = CyberCyan,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.isMoe) "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B (${String.format(Locale.US, "%.1f", model.activeParamsBillion)}B active)" else "${String.format(Locale.US, "%.1f", model.totalParamsBillion)}B params",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Context length
                Text(
                    text = "${model.contextLengthTokens / 1024}K ctx",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Hardware Compatibility Pill
            if (compatibility != null) {
                val (fitText, fitColor) = when (compatibility.fitGrade) {
                    FitGrade.EXCELLENT -> Pair("✓ Fits (${compatibility.profileName})", FitExcellent)
                    FitGrade.BORDERLINE -> Pair("⚠ ${compatibility.offloadPercentage}% GPU (${compatibility.profileName})", FitBorderline)
                    FitGrade.INSUFFICIENT -> Pair("✕ Exceeds VRAM (${compatibility.profileName})", FitInsufficient)
                    FitGrade.UNKNOWN -> Pair("? Compatibility Unknown", TextMuted)
                }
                GlassBadge(text = fitText, color = fitColor)
            }
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
        else -> count.toString()
    }
}
