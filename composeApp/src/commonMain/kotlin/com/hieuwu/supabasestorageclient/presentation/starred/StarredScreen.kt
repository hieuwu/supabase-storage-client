package com.hieuwu.supabasestorageclient.presentation.starred

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(
    onNavigateToBucket: (String) -> Unit,
    onNavigateToFolder: (String, String) -> Unit,
    onNavigateToFile: (String, String, String?) -> Unit,
    viewModel: StarredViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadStarredItems() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.StarOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Starred Items",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your starred files and folders will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.onClearAllClick() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All")
                    }
                }
                if (uiState.viewMode == ViewMode.LIST) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.items) { item ->
                            StarredItemRow(
                                item = item,
                                onClick = {
                                    when {
                                        item.isBucket -> onNavigateToBucket(item.bucketId)
                                        item.isFolder -> onNavigateToFolder(item.bucketId, item.path ?: "")
                                        else -> onNavigateToFile(
                                            item.bucketId,
                                            item.itemName,
                                            item.path?.substringBeforeLast("/", "")
                                        )
                                    }
                                },
                                onUnstar = { viewModel.unstarItem(item.itemId) }
                            )
                            HorizontalDivider()
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.items) { item ->
                            StarredItemGrid(
                                item = item,
                                onClick = {
                                    when {
                                        item.isBucket -> onNavigateToBucket(item.bucketId)
                                        item.isFolder -> onNavigateToFolder(item.bucketId, item.path ?: "")
                                        else -> onNavigateToFile(
                                            item.bucketId,
                                            item.itemName,
                                            item.path?.substringBeforeLast("/", "")
                                        )
                                    }
                                },
                                onUnstar = { viewModel.unstarItem(item.itemId) }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (uiState.showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAllConfirmation() },
            title = { Text("Clear All Starred") },
            text = { Text("Are you sure you want to remove all items from starred?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearAll() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearAllConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StarredItemRow(
    item: StarredItem,
    onClick: () -> Unit,
    onUnstar: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.itemName) },
        supportingContent = {
            val typeStr = when {
                item.isBucket -> "Bucket"
                item.isFolder -> "Folder"
                else -> "File"
            }
            Text("$typeStr • ${item.bucketId}${if (item.path != null) "/${item.path}" else ""}")
        },
        leadingContent = {
            val icon = when {
                item.isBucket -> Icons.Default.Storage
                item.isFolder -> Icons.Default.Folder
                else -> Icons.Default.InsertDriveFile
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (item.isFolder || item.isBucket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onUnstar) {
                    Icon(Icons.Default.Star, contentDescription = "Unstar", tint = MaterialTheme.colorScheme.primary)
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun StarredItemGrid(
    item: StarredItem,
    onClick: () -> Unit,
    onUnstar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onUnstar,
                    modifier = Modifier.align(Alignment.TopStart).size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Unstar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val icon = when {
                item.isBucket -> Icons.Default.Storage
                item.isFolder -> Icons.Default.Folder
                else -> Icons.Default.InsertDriveFile
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (item.isFolder || item.isBucket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp).padding(vertical = 8.dp)
            )

            Text(
                text = item.itemName,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            val typeStr = when {
                item.isBucket -> "Bucket"
                item.isFolder -> "Folder"
                else -> "File"
            }
            Text(
                text = typeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
