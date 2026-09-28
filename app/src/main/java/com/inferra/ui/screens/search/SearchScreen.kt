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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.ModelTask
import com.inferra.ui.components.GlassChip
import com.inferra.ui.components.GlassTextField
import com.inferra.ui.components.LiquidGlassBackground
import com.inferra.ui.components.ModelCard
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberViolet
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
            // Header Search Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "SEARCH & FILTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    placeholderText = "Search by model, architecture, GGUF, context...",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Task Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        GlassChip(
                            text = "⚡ Coding",
                            isSelected = state.selectedTask == ModelTask.CODING,
                            onClick = { viewModel.onTaskFilterSelected(ModelTask.CODING) }
                        )
                    }
                    item {
                        GlassChip(
                            text = "🧠 Reasoning",
                            isSelected = state.selectedTask == ModelTask.REASONING,
                            onClick = { viewModel.onTaskFilterSelected(ModelTask.REASONING) }
                        )
                    }
                    item {
                        GlassChip(
                            text = "👁 Vision",
                            isSelected = state.selectedTask == ModelTask.VISION,
                            onClick = { viewModel.onTaskFilterSelected(ModelTask.VISION) }
                        )
                    }
                    item {
                        GlassChip(
                            text = "🛠 Tool Calling",
                            isSelected = state.selectedTask == ModelTask.TOOL_CALLING,
                            onClick = { viewModel.onTaskFilterSelected(ModelTask.TOOL_CALLING) }
                        )
                    }
                    item {
                        GlassChip(
                            text = "📦 GGUF Only",
                            isSelected = state.isGgufOnly,
                            onClick = { viewModel.onGgufToggle() },
                            accentColor = CyberViolet
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
                    text = "${state.searchResults.size} models found",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results List
            if (state.searchResults.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.height(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No models match your query and filters.", color = TextSecondary, fontSize = 14.sp)
                        Text(text = "Try clearing filters or broadening search term.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.searchResults, key = { it.id }) { model ->
                        ModelCard(
                            model = model,
                            compatibility = state.compatibilityMap[model.id],
                            onClick = { onNavigateToModel(model.id) }
                        )
                    }
                }
            }
        }
    }
}
