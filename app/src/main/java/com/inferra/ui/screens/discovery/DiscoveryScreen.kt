package com.inferra.ui.screens.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.EmptyReason
import com.inferra.domain.model.UiState
import com.inferra.ui.components.ExecutiveEmptyState
import com.inferra.ui.components.ExecutiveModelCard

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onNavigateToModel: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToHardware: () -> Unit = {}
) {
    val catalogState by viewModel.catalogUiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Model Intelligence",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Executive Model Discovery & Evaluation Explorer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FTS Search Field
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            text = "Search models, organizations, or families...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Parameter Quick Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterPill(
                            text = "All Models",
                            isSelected = (filterState.minParamsBillion == null) && (filterState.maxParamsBillion == null) && (filterState.isOpenWeightsOnly == null),
                        ) { viewModel.resetFilters() }
                    }
                    item {
                        FilterPill(
                            text = "Open Weights",
                            isSelected = filterState.isOpenWeightsOnly == true,
                            onClick = { viewModel.onOpenWeightsToggled(if (filterState.isOpenWeightsOnly == true) null else true) }
                        )
                    }
                    item {
                        FilterPill(
                            text = "Frontier (>50B)",
                            isSelected = filterState.minParamsBillion == 50,
                            onClick = { viewModel.onParamsRangeChanged(if (filterState.minParamsBillion == 50) null else 50, null) }
                        )
                    }
                    item {
                        FilterPill(
                            text = "Compact (<=32B)",
                            isSelected = filterState.maxParamsBillion == 32,
                            onClick = { viewModel.onParamsRangeChanged(null, if (filterState.maxParamsBillion == 32) null else 32) }
                        )
                    }
                    item {
                        FilterPill(
                            text = "Long Context (>=128K)",
                            isSelected = filterState.minContextLength == 128000,
                            onClick = { viewModel.onMinContextChanged(if (filterState.minContextLength == 128000) null else 128000) }
                        )
                    }
                }
            }

            // Catalog Content Body
            when (val state = catalogState) {
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
                        reason = state.reason,
                        onAction = { viewModel.resetFilters() }
                    )
                }

                is UiState.Error -> {
                    ExecutiveEmptyState(
                        reason = EmptyReason.CORRUPTED_CACHE,
                        onAction = { viewModel.resetFilters() }
                    )
                }

                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.data, key = { it.id }) { model ->
                            val topScores by viewModel.getTopScoresForModel(model.id).collectAsState()

                            ExecutiveModelCard(
                                model = model,
                                topScores = topScores,
                                onClick = { onNavigateToModel(model.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}
