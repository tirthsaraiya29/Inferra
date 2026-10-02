package com.inferra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.data.local.AndroidArtifactEntity
import com.inferra.data.local.AndroidModelEntity
import com.inferra.domain.model.BenchmarkDomainCategory
import com.inferra.domain.model.BenchmarkScoreUiModel
import com.inferra.domain.model.EmptyReason
import com.inferra.ui.theme.SageGreen
import com.inferra.ui.theme.SageGreenDark
import com.inferra.ui.theme.SlateBlue
import com.inferra.ui.theme.WarmAmber

@Composable
fun ExecutiveModelCard(
    model: AndroidModelEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topScores: List<BenchmarkScoreUiModel> = emptyList(),
) {
    val isOpenWeights = model.isOpenWeights == 1

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = model.organization.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = model.organization,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOpenWeights) SageGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        0.5.dp,
                        if (isOpenWeights) SageGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOpenWeights) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isOpenWeights) SageGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOpenWeights) "Open Weights" else "Proprietary",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOpenWeights) SageGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricPill(
                    label = "Parameters",
                    value = formatParams(model.parameterCount)
                )
                MetricPill(
                    label = "Context Window",
                    value = formatContext(model.contextLength)
                )
                MetricPill(
                    label = "Type",
                    value = model.modelType
                )
            }

            if (topScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    topScores.take(3).forEach { (name, scoreFormatted) ->
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = scoreFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OfficialReleaseCta(
    officialUrl: String,
    onLaunchUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onLaunchUrl(officialUrl) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Official Release & Model Card",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun QuantizationBadge(
    artifact: AndroidArtifactEntity,
    onArtifactClick: (AndroidArtifactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onArtifactClick(artifact) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "${artifact.format} • ${artifact.quantization ?: "Standard"}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatFileSize(artifact.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BenchmarkDomainPill(
    category: BenchmarkDomainCategory,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (category) {
        BenchmarkDomainCategory.REASONING_SCIENCE -> SageGreen to category.displayName
        BenchmarkDomainCategory.SOFTWARE_ENGINEERING -> SlateBlue to category.displayName
        BenchmarkDomainCategory.AGENTS_TOOLS -> WarmAmber to category.displayName
        else -> MaterialTheme.colorScheme.primary to category.displayName
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}

@Composable
fun ExecutiveEmptyState(
    reason: EmptyReason,
    modifier: Modifier = Modifier,
    customTitle: String? = null,
    customBody: String? = null,
    customButtonLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val (defaultTitle, defaultBody, defaultButton) = when (reason) {
        EmptyReason.NO_RESULTS -> Triple(
            "No Matching Models Found",
            "Try broadening your search query or adjusting parameter filters.",
            "Reset Filters"
        )
        EmptyReason.NO_BENCHMARKS -> Triple(
            "No Benchmark Records Available",
            "Detailed evaluation records for this checkpoint are currently updating.",
            "Refresh"
        )
        EmptyReason.NO_COMPARISON_MODELS -> Triple(
            "Select Models to Compare",
            "Select 2 or more models from the catalog to inspect differential metrics.",
            "Browse Catalog"
        )
        EmptyReason.CORRUPTED_CACHE -> Triple(
            "Database Cache Rebuilding Required",
            "The local model index requires a quick integrity check and cache reconstruction.",
            "Rebuild Catalog Cache"
        )
    }

    val title = customTitle ?: defaultTitle
    val body = customBody ?: defaultBody
    val buttonLabel = customButtonLabel ?: defaultButton

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (reason == EmptyReason.CORRUPTED_CACHE) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = buttonLabel)
            }
        }
    }
}

private fun formatParams(params: Int?): String {
    if ((params == null) || (params <= 0)) return "N/A"
    return "${params}B"
}

private fun formatContext(contextLength: Int?): String {
    if ((contextLength == null) || (contextLength <= 0)) return "N/A"
    return if (contextLength >= 1000000) {
        "${contextLength / 1000000}M tokens"
    } else if (contextLength >= 1000) {
        "${contextLength / 1000}K tokens"
    } else {
        "$contextLength tokens"
    }
}

private fun formatFileSize(sizeBytes: Long?): String {
    if (sizeBytes == null || sizeBytes <= 0) return "Unknown size"
    val gb = sizeBytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(sizeBytes.toDouble() / (1024 * 1024))
}
