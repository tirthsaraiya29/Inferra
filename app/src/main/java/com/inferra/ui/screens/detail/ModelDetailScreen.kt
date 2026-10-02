package com.inferra.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.data.local.AndroidArtifactEntity
import com.inferra.data.local.AndroidModelEntity
import com.inferra.data.local.AndroidProviderPricingEntity
import com.inferra.data.local.ModelWithDetails
import com.inferra.domain.model.BenchmarkDomainCategory
import com.inferra.domain.model.BenchmarkScoreUiModel
import com.inferra.domain.model.EmptyReason
import com.inferra.domain.model.ProvenanceInfo
import com.inferra.domain.model.ProvenanceResolver
import com.inferra.domain.model.UiState
import com.inferra.ui.components.ExecutiveEmptyState
import com.inferra.ui.components.MetricPill
import com.inferra.ui.components.OfficialReleaseCta
import com.inferra.ui.components.QuantizationBadge
import com.inferra.ui.theme.SageGreen
import com.inferra.ui.theme.SageGreenDark

@Composable
fun ModelDetailScreen(
    viewModel: ModelDetailViewModel,
    onBackClick: () -> Unit,
    onLaunchUrl: (String) -> Unit
) {
    val state by viewModel.viewState.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding)
        ) {
            // Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Model Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = { viewModel.toggleWatchlist() }) {
                    Icon(
                        imageVector = if (state.isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (state.isWatchlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            when (val detailsState = state.detailsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }

                is UiState.Empty -> {
                    ExecutiveEmptyState(
                        reason = detailsState.reason,
                        onAction = onBackClick
                    )
                }

                is UiState.Error -> {
                    ExecutiveEmptyState(
                        reason = EmptyReason.NO_RESULTS,
                        onAction = onBackClick
                    )
                }

                is UiState.Success -> {
                    ModelDetailContent(
                        details = detailsState.data,
                        scores = state.benchmarkScores,
                        provenance = state.provenanceInfo,
                        onLaunchUrl = onLaunchUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelDetailContent(
    details: ModelWithDetails,
    scores: List<BenchmarkScoreUiModel>,
    provenance: ProvenanceInfo?,
    onLaunchUrl: (String) -> Unit
) {
    val model = details.model
    val artifacts = details.artifacts
    val pricingList = details.providerPricing

    val categories = remember {
        listOf(
            BenchmarkDomainCategory.REASONING_SCIENCE,
            BenchmarkDomainCategory.SOFTWARE_ENGINEERING,
            BenchmarkDomainCategory.AGENTS_TOOLS,
            BenchmarkDomainCategory.CONTEXT_MULTIMODAL,
            BenchmarkDomainCategory.HUMAN_PREFERENCE
        )
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Executive Header Card
        item {
            ExecutiveHeaderCard(
                model = model,
                provenance = provenance
            )
        }

        // Official Release CTA
        provenance?.let { prov ->
            item {
                OfficialReleaseCta(
                    officialUrl = prov.officialReleaseUrl,
                    onLaunchUrl = onLaunchUrl
                )
            }
        }

        // Tabbed Benchmark Breakdown Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Benchmark Suite Performance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Standardized evaluation metrics across major test harnesses. Unmeasured metrics render as '—'.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        categories.forEachIndexed { index, cat ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val activeCategory = categories[selectedTabIndex]
                    val categoryScores = remember(scores, activeCategory) {
                        scores.filter { it.category == activeCategory }
                    }

                    if (categoryScores.isEmpty()) {
                        Text(
                            text = "No recorded evaluations in this test harness category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categoryScores.forEach { score ->
                                BenchmarkScoreRow(score = score)
                            }
                        }
                    }
                }
            }
        }

        // Artifact & Quantization Upstream Mirrors
        if (artifacts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quantized Artifacts & Weights",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap any artifact badge to inspect upstream file trees and mirrors.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            artifacts.forEach { artifact ->
                                QuantizationBadge(
                                    artifact = artifact,
                                    onArtifactClick = { art ->
                                        val url = ProvenanceResolver.resolveArtifactUrl(art.repositoryId, art.filePath)
                                        onLaunchUrl(url)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Provider Pricing Section
        if (pricingList.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "API Provider Pricing & Limits",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            pricingList.forEach { price ->
                                ProviderPricingRow(price = price)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ExecutiveHeaderCard(
    model: AndroidModelEntity,
    provenance: ProvenanceInfo? = null,
) {
    val isOpenWeights = model.isOpenWeights == 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = model.organization.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = model.organization,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "Parameters",
                    value = if ((model.parameterCount != null) && (model.parameterCount > 0)) "${model.parameterCount}B" else "N/A"
                )
                MetricPill(
                    label = "Context Window",
                    value = if ((model.contextLength != null) && (model.contextLength >= 1000)) "${model.contextLength / 1000}K" else "N/A"
                )
                MetricPill(
                    label = "License",
                    value = model.license ?: "Standard"
                )
            }
        }
    }
}

@Composable
private fun BenchmarkScoreRow(score: BenchmarkScoreUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = score.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = score.metricName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = score.scoreFormatted,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (score.scoreFormatted == "—") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProviderPricingRow(price: AndroidProviderPricingEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = price.providerName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Context Limit: ${if (price.contextWindow != null) "${price.contextWindow / 1000}K" else "N/A"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "In: $${price.inputCostPerM ?: 0.00}/M",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Out: $${price.outputCostPerM ?: 0.00}/M",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
