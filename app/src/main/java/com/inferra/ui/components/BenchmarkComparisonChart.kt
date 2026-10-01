package com.inferra.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.BenchmarkResult
import java.util.Locale

@Composable
fun BenchmarkComparisonChart(
    benchmarks: List<BenchmarkResult>,
    modifier: Modifier = Modifier
) {
    if (benchmarks.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CANONICAL BENCHMARK EVIDENCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Empirical Evaluation Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            benchmarks.forEach { bench ->
                BenchmarkItemRow(bench = bench)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun BenchmarkItemRow(bench: BenchmarkResult) {
    var showProvenanceDetail by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .clickable { showProvenanceDetail = !showProvenanceDetail }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shortTitle = bench.id.removePrefix("res-").replace('-', ' ').uppercase(Locale.US)
            Text(
                text = shortTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bench.provenance.verifiedByInferra) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Provenance",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.height(16.dp).width(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${bench.score} / ${bench.maxScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        val progress = (bench.score / bench.maxScore).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (progress > 0.8f) Color(0xFF10B981) else Color(0xFF3B82F6),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        AnimatedVisibility(visible = showProvenanceDetail) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(14.dp).width(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Publisher: ${bench.provenance.publisher} (${bench.provenance.provenanceType.name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
