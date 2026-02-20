package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
            onRefresh = { viewModel.loadBuckets() },
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.buckets) { bucket ->
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
                                                containerColor = Color(0xFFE8F5E9),
                                                labelColor = Color(0xFF2E7D32)
                                            ),
                                            border = SuggestionChipDefaults.suggestionChipBorder(
                                                enabled = true,
                                                borderColor = Color(0xFF81C784)
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
                                                viewModel.onEmptyBucketClick(bucket)
                                            },
                                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete bucket") },
                                            onClick = {
                                                showMenu = false
                                                viewModel.onDeleteBucketClick(bucket)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onBucketClick(bucket.id) }
                        )
                        HorizontalDivider()
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

