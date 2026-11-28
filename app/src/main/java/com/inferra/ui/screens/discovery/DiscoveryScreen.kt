package com.inferra.ui.screens.discovery

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UiState
import com.inferra.ui.components.RepoCard

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onNavigateToRepo: (String, RepoType) -> Unit,
    onNavigateToSearch: (String, RepoType) -> Unit,
    onNavigateToType: (RepoType) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
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
            // App Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Hugging Face Hub",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Explore Models, Datasets & Spaces",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar Trigger
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            text = "Search Hugging Face Hub...",
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
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            when (val uiState = state) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.userMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.loadDiscoveryFeed() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val feed = uiState.data

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Section: Trending Models
                        if (feed.trendingModels.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Trending Models",
                                    onSeeAll = { onNavigateToType(RepoType.MODEL) }
                                )
                                HorizontalRepoList(
                                    items = feed.trendingModels,
                                    onItemClick = { onNavigateToRepo(it.id, RepoType.MODEL) }
                                )
                            }
                        }

                        // Section: Popular Datasets
                        if (feed.popularDatasets.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Popular Datasets",
                                    onSeeAll = { onNavigateToType(RepoType.DATASET) }
                                )
                                HorizontalRepoList(
                                    items = feed.popularDatasets,
                                    onItemClick = { onNavigateToRepo(it.id, RepoType.DATASET) }
                                )
                            }
                        }

                        // Section: Trending Spaces
                        if (feed.trendingSpaces.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Trending Spaces",
                                    onSeeAll = { onNavigateToType(RepoType.SPACE) }
                                )
                                HorizontalRepoList(
                                    items = feed.trendingSpaces,
                                    onItemClick = { onNavigateToRepo(it.id, RepoType.SPACE) }
                                )
                            }
                        }

                        // Section: Recently Updated
                        if (feed.recentModels.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Recently Updated Models",
                                    onSeeAll = { onNavigateToType(RepoType.MODEL) }
                                )
                                HorizontalRepoList(
                                    items = feed.recentModels,
                                    onItemClick = { onNavigateToRepo(it.id, RepoType.MODEL) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onSeeAll) {
            Text(text = "See All", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HorizontalRepoList(
    items: List<RepoItem>,
    onItemClick: (RepoItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items, key = { it.id }) { item ->
            RepoCard(
                repo = item,
                onClick = { onItemClick(item) },
                modifier = Modifier.width(280.dp)
            )
        }
    }
}
