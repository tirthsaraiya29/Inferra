package com.inferra.ui.screens.detail

import android.content.Context
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.domain.model.RepoDetail
import com.inferra.domain.model.UiState
import com.inferra.ui.components.FileTreeRow
import com.inferra.ui.components.MarkdownViewer
import com.inferra.ui.components.RepoHeaderCard

@Composable
fun RepoDetailScreen(
    viewModel: RepoDetailViewModel,
    onBackClick: () -> Unit,
    onLaunchUrl: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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
            // Top Navigation Bar
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
                    text = "Repository Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val detailData = (state.detailState as? UiState.Success)?.data
                IconButton(onClick = {
                    detailData?.repo?.let { viewModel.toggleBookmark(it) }
                }) {
                    Icon(
                        imageVector = if (state.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            when (val detailUi = state.detailState) {
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
                            text = detailUi.message,
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
                                text = detailUi.userMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.loadDetail() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val detail = detailUi.data

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Card
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            RepoHeaderCard(
                                repo = detail.repo,
                                isBookmarked = state.isBookmarked,
                                onBookmarkToggle = { viewModel.toggleBookmark(detail.repo) },
                                onShare = {
                                    shareUrl(context, "https://huggingface.co/${detail.repo.id}")
                                },
                                onOpenHf = {
                                    onLaunchUrl("https://huggingface.co/${detail.repo.id}")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab Row
                        val tabs = listOf("README", "Files & Tree", "Metadata")
                        PrimaryTabRow(
                            selectedTabIndex = state.selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = state.selectedTab == index,
                                    onClick = { viewModel.selectTab(index) },
                                    text = { Text(text = title, fontSize = 13.sp) }
                                )
                            }
                        }

                        // Tab Content
                        when (state.selectedTab) {
                            0 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    item {
                                        val readme = detail.readmeContent
                                        if (!readme.isNullOrBlank()) {
                                            MarkdownViewer(
                                                markdown = readme,
                                                onLinkClick = onLaunchUrl
                                            )
                                        } else {
                                            Text(
                                                text = "No README file found for this repository.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (state.isTreeLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(state.treeItems, key = { it.path }) { file ->
                                            FileTreeRow(
                                                item = file,
                                                onFileClick = { clicked ->
                                                    if (clicked.type == "directory") {
                                                        viewModel.loadTreePath(path = clicked.path)
                                                    } else {
                                                        viewModel.previewFileContent(clicked.path)
                                                    }
                                                },
                                                onDownloadClick = { downloadTarget ->
                                                    viewModel.downloadFile(context, detail.repo, downloadTarget)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            2 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        MetadataCard(repo = detail.repo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // File Text Preview Dialog
        state.selectedFilePreview?.let { (fileName, textContent) ->
            AlertDialog(
                onDismissRequest = { viewModel.clearFilePreview() },
                title = { Text(text = fileName, style = MaterialTheme.typography.titleMedium) },
                text = {
                    Box(modifier = Modifier.height(300.dp)) {
                        MarkdownViewer(markdown = textContent)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearFilePreview() }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun MetadataCard(repo: com.inferra.domain.model.RepoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Repository Information", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            MetaRow("Repository ID", repo.id)
            MetaRow("Repository Type", repo.type.displayName)
            MetaRow("Author / Org", repo.author)
            repo.pipelineTag?.let { MetaRow("Pipeline Tag", it) }
            repo.libraryName?.let { MetaRow("Library Name", it) }
            repo.sdk?.let { MetaRow("Spaces SDK", it) }
            repo.license?.let { MetaRow("License", it) }
            repo.createdAt?.let { MetaRow("Created At", it) }
            repo.lastModified?.let { MetaRow("Last Modified", it) }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun shareUrl(context: Context, url: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, url)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Repository"))
}
