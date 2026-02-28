package com.hieuwu.supabasestorageclient.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import org.koin.compose.viewmodel.koinViewModel

import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    bucketId: String?,
    onBack: () -> Unit,
    onNavigateToBucket: (String) -> Unit,
    onNavigateToFolder: (String, String) -> Unit,
    onNavigateToFile: (String, String, String?) -> Unit,
    viewModel: SearchViewModel = koinViewModel { parametersOf(bucketId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    val placeholder = if (bucketId == null) "Search in Buckets..." else "Search in this bucket..."
                    Text(placeholder) 
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.query.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (bucketId == null) Icons.Default.Storage else Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (bucketId == null) "Searching in Buckets" else "Searching in current bucket",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.searchType == SearchType.BUCKET) {
                        items(uiState.buckets) { bucket ->
                            BucketResultRow(
                                bucket = bucket,
                                onClick = { onNavigateToBucket(bucket.id) },
                                onToggleStar = { viewModel.toggleStar(bucket) }
                            )
                            HorizontalDivider()
                        }
                    } else {
                        items(uiState.storageItems) { result ->
                            StorageItemResultRow(
                                item = result.item,
                                onClick = {
                                    if (result.item.isFolder) {
                                        onNavigateToFolder(result.bucketId, result.item.name)
                                    } else {
                                        onNavigateToFile(result.bucketId, result.item.name, null)
                                    }
                                },
                                onToggleStar = { viewModel.toggleStar(result.item, result.bucketId) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
}

@Composable
fun BucketResultRow(
    bucket: Bucket,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    ListItem(
        headlineContent = { Text(bucket.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            if (bucket.public) {
                Text("Public", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Private")
            }
        },
        leadingContent = {
            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            IconButton(onClick = onToggleStar) {
                Icon(
                    if (bucket.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = if (bucket.isStarred) "Unstar" else "Star",
                    tint = if (bucket.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun StorageItemResultRow(
    item: StorageItem,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
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
            val extension = item.name.substringAfterLast(".", "").lowercase()
            val icon = when {
                item.isFolder -> Icons.Default.Folder
                extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Default.Image
                extension in listOf("mp4", "mov", "avi", "mkv", "webm") -> Icons.Default.VideoLibrary
                extension == "pdf" -> Icons.Default.PictureAsPdf
                else -> Icons.Default.InsertDriveFile
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onToggleStar) {
                Icon(
                    if (item.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = if (item.isStarred) "Unstar" else "Star",
                    tint = if (item.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
