package com.hieuwu.supabasestorageclient.presentation.bucket

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketScreen(
    bucketId: String,
    path: String?,
    onBack: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToPath: (String) -> Unit,
    viewModel: BucketViewModel = koinViewModel(parameters = { parametersOf(bucketId, path) }),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bucketId, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Breadcrumbs(
                currentPath = path.orEmpty(),
                onPathClick = onNavigateToPath
            )
            
            HorizontalDivider()

            Box(modifier = Modifier.weight(1.0f).fillMaxSize()) {
                if (uiState.isLoading && uiState.items.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.error != null && uiState.items.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadContents() }) {
                            Text("Retry")
                        }
                    }
                } else {
                    val state = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadContents() },
                        state = state,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.items) { item ->
                                StorageItemRow(
                                    item = item,
                                    onClick = {
                                        if (item.isFolder) {
                                            val newPath = if (path.isNullOrEmpty()) {
                                                item.name
                                            } else {
                                                "$path/${item.name}"
                                            }
                                            onNavigateToFolder(newPath)
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun Breadcrumbs(
    currentPath: String,
    onPathClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "root",
            modifier = Modifier.clickable { onPathClick("") },
            color = if (currentPath.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
        
        if (currentPath.isNotEmpty()) {
            val parts = currentPath.split("/")
            var accumulatedPath = ""
            parts.forEachIndexed { index, part ->
                accumulatedPath = if (accumulatedPath.isEmpty()) part else "$accumulatedPath/$part"
                val pathSnapshot = accumulatedPath
                
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = part,
                    modifier = Modifier.clickable { onPathClick(pathSnapshot) },
                    color = if (index == parts.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun StorageItemRow(
    item: StorageItem,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            if (!item.isFolder) {
                val sizeStr = item.size?.let { "${it / 1024} KB" } ?: ""
                val dateStr = item.updatedAt?.toString()?.take(10) ?: ""
                Text("$sizeStr • $dateStr")
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
