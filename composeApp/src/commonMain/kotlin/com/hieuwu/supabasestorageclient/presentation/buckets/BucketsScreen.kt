package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketsScreen(
    onBucketClick: (String) -> Unit,
    viewModel: BucketsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshBuckets() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.buckets.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && uiState.buckets.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadBuckets() }) {
                        Text("Retry")
                    }
                }
            } else {
                if (uiState.viewMode == ViewMode.LIST) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.buckets) { bucket ->
                            BucketListItem(
                                bucket = bucket,
                                onClick = { onBucketClick(bucket.id) },
                                onToggleStar = { viewModel.toggleStar(bucket) },
                                onEmptyClick = { viewModel.onEmptyBucketClick(bucket) },
                                onDeleteClick = { viewModel.onDeleteBucketClick(bucket) }
                            )
                            HorizontalDivider()
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.buckets) { bucket ->
                            BucketGridItem(
                                bucket = bucket,
                                onClick = { onBucketClick(bucket.id) },
                                onToggleStar = { viewModel.toggleStar(bucket) },
                                onEmptyClick = { viewModel.onEmptyBucketClick(bucket) },
                                onDeleteClick = { viewModel.onDeleteBucketClick(bucket) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.buckets.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    if (uiState.showEmptyConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Empty Bucket") },
            text = { Text("Are you sure you want to empty the bucket '${uiState.selectedBucket?.name}'? All files will be deleted.") },
            confirmButton = {
                Button(onClick = { viewModel.confirmEmptyBucket() }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Delete Bucket") },
            text = { Text("Are you sure you want to delete the bucket '${uiState.selectedBucket?.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteBucket() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Cancel")
                }
            }
        )
    }

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
}

@Composable
fun BucketListItem(
    bucket: com.hieuwu.supabasestorageclient.domain.model.Bucket,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onEmptyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = {
            Text(bucket.name, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val mimeTypes = bucket.allowedMimeTypes?.joinToString(", ") ?: "All"
                val sizeLimit = if (bucket.fileSizeLimit != null) formatSize(bucket.fileSizeLimit) else "None"

                Text("Allowed Types: $mimeTypes", style = MaterialTheme.typography.labelSmall)
                Text("Size Limit: $sizeLimit", style = MaterialTheme.typography.labelSmall)
                Text("Created: ${bucket.createdAt}", style = MaterialTheme.typography.labelSmall)

                if (bucket.public) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Public", style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            labelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (bucket.isStarred) "Unstar" else "Star") },
                        onClick = {
                            showMenu = false
                            onToggleStar()
                        },
                        leadingIcon = {
                            Icon(
                                if (bucket.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = if (bucket.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Update bucket") },
                        onClick = {
                            showMenu = false
                            // Leave update bucket for now
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Empty bucket") },
                        onClick = {
                            showMenu = false
                            onEmptyClick()
                        },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete bucket") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun BucketGridItem(
    bucket: com.hieuwu.supabasestorageclient.domain.model.Bucket,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onEmptyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
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
                    onClick = onToggleStar,
                    modifier = Modifier.align(Alignment.TopStart).size(24.dp)
                ) {
                    Icon(
                        if (bucket.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (bucket.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Empty bucket") },
                            onClick = {
                                showMenu = false
                                onEmptyClick()
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete bucket") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }

            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp).padding(vertical = 8.dp)
            )

            Text(
                text = bucket.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            if (bucket.public) {
                Text(
                    "Public",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "${gb.toLong()} GB"
        mb >= 1 -> "${mb.toLong()} MB"
        kb >= 1 -> "${kb.toLong()} KB"
        else -> "$bytes Bytes"
    }
}

