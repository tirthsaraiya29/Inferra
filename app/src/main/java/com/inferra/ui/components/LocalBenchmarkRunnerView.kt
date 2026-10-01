package com.inferra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.data.repository.LlamaCppEngineAdapter
import com.inferra.data.repository.OnDeviceBenchmarkRunner
import com.inferra.domain.model.LocalBenchmarkResult
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LocalBenchmarkRunnerView(
    canonicalId: String,
    quantType: String,
    localFilePath: String?,
    onResultGenerated: (LocalBenchmarkResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<LocalBenchmarkResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ON-DEVICE BENCHMARK RUNNER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Empirical Local Performance Measurement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (localFilePath.isNullOrBlank()) {
                Text(
                    text = "Model file must be downloaded to this Android device before on-device benchmark execution.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isRunning = true
                            errorMessage = null
                            try {
                                val runner = OnDeviceBenchmarkRunner(engine = LlamaCppEngineAdapter())
                                val res = runner.runBenchmark(
                                    modelPath = localFilePath,
                                    canonicalId = canonicalId,
                                    quantType = quantType
                                )
                                lastResult = res
                                onResultGenerated(res)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Benchmark execution failed"
                            } finally {
                                isRunning = false
                            }
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Executing Local Inference Benchmark...")
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run On-Device Benchmark")
                    }
                }
            }

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            AnimatedVisibility(visible = lastResult != null) {
                lastResult?.let { res ->
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Latest Measurement Result", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        MetricRow(label = "Time To First Token (TTFT)", value = "${String.format(Locale.US, "%.1f", res.ttftMs)} ms")
                        MetricRow(label = "Prefill Speed (Prompt Processing)", value = "${String.format(Locale.US, "%.1f", res.prefillTokensPerSec)} tok/s")
                        MetricRow(label = "Decode Speed (Token Generation)", value = "${String.format(Locale.US, "%.1f", res.decodeTokensPerSec)} tok/s")
                        MetricRow(label = "Backend Runtime", value = res.backendName)
                        MetricRow(label = "Peak Memory Usage", value = "${res.peakRamMb} MB RAM / ${res.peakVramMb} MB VRAM")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
