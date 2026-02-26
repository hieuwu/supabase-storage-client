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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToBucket: (String) -> Unit,
    onNavigateToFolder: (String, String) -> Unit,
    onNavigateToFile: (String, String, String?) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
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
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchType.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SearchType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.onSearchTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.searchType == SearchType.BUCKET) {
                    items(uiState.buckets) { bucket ->
                        BucketResultRow(
                            bucket = bucket,
                            onClick = { onNavigateToBucket(bucket.id) }
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
                            }
                        )
                        HorizontalDivider()
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
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(bucket.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            if (bucket.public) {
                Text("Public", color = Color(0xFF2E7D32))
            } else {
                Text("Private")
            }
        },
        leadingContent = {
            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun StorageItemResultRow(
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
        modifier = Modifier.clickable { onClick() }
    )
}
