package com.inferra.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.ModelTask
import com.inferra.ui.components.GlassButton
import com.inferra.ui.components.GlassChip
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.components.ModelCard
import com.inferra.ui.theme.AccentAzure
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary
import com.inferra.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    initialQuery: String = "",
    onNavigateToModel: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && state.query.isBlank()) {
            viewModel.onQueryChanged(initialQuery)
        }
    }

    LiquidGlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Search",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Find models by name, architecture, capability, or specs",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    placeholderText = "Try 'coding', '32B', 'Qwen', or 'GGUF'...",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AccentAzure)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Natural language suggestion chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        GlassChip(
                            text = "fast coding models",
                            isSelected = state.query == "coding",
                            onClick = { viewModel.onQueryChanged("coding") }
                        )
                    }
                    item {
                        GlassChip(
                            text = "models under 8GB VRAM",
                            isSelected = state.query == "8B",
                            onClick = { viewModel.onQueryChanged("8B") }
                        )
                    }
                    item {
                        GlassChip(
                            text = "vision models",
                            isSelected = state.selectedTask == ModelTask.VISION,
                            onClick = { viewModel.onTaskFilterSelected(ModelTask.VISION) }
                        )
                    }
                    item {
                        GlassChip(
                            text = "long-context models",
                            isSelected = state.query == "context",
                            onClick = { viewModel.onQueryChanged("context") }
                        )
                    }
                    item {
                        GlassChip(
                            text = "GGUF Only",
                            isSelected = state.isGgufOnly,
                            onClick = { viewModel.onGgufToggle() }
                        )
                    }
                }
            }

            // Results count label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.searchResults.isNotEmpty()) "${state.searchResults.size} models found" else "",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), color = AccentAzure, strokeWidth = 2.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results List
            if ((state.errorMessage != null) && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unable to connect to Hugging Face",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.errorMessage ?: "Please check your network connection.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassButton(onClick = { viewModel.retry() }) {
                            Text(text = "Retry", fontSize = 14.sp)
                        }
                    }
                }
            } else if (state.searchResults.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No models match your search.",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching for a different keyword or selecting a suggestion above.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(items = state.searchResults, key = { _, item -> item.id }) { idx, model ->
                        if ((idx >= state.searchResults.size - 2) && !state.isLoadingMore && state.canLoadMore) {
                            LaunchedEffect(idx) {
                                viewModel.loadNextPage()
                            }
                        }
                        ModelCard(
                            model = model,
                            compatibility = state.compatibilityMap[model.id],
                        ) { onNavigateToModel(model.id) }
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentAzure, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
