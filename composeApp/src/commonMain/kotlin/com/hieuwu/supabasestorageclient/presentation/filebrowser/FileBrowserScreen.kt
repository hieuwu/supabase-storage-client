@file:OptIn(ExperimentalMaterial3Api::class)

package com.hieuwu.supabasestorageclient.presentation.filebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketScreen(
    bucketId: String,
    path: String?,
    onBack: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToFile: (String, String, String?) -> Unit,
    viewModel: FileBrowserViewModel = koinViewModel(parameters = { parametersOf(bucketId, path) })
) {
    val uiState by viewModel.uiState.collectAsState()

    var itemToRename by remember { mutableStateOf<StorageItem?>(null) }
    var itemToMove by remember { mutableStateOf<StorageItem?>(null) }
    var itemToDelete by remember { mutableStateOf<StorageItem?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            viewModel.clearMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Breadcrumbs(
            currentPath = path.orEmpty(),
            onPathClick = onNavigateToFolder,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.isLoading && uiState.items.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.refreshContents() }
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.items) { item ->
                            StorageItemRow(
                                item = item,
                                onClick = {
                                    if (item.isFolder) {
                                        val nextPath = if (path.isNullOrEmpty()) item.name else "$path/${item.name}"
                                        onNavigateToFolder(nextPath)
                                    } else {
                                        onNavigateToFile(bucketId, item.name, path)
                                    }
                                },
                                onRename = { itemToRename = item },
                                onMove = { itemToMove = item },
                                onDelete = { itemToDelete = item },
                                onGetUrl = { viewModel.getPublicUrl(item.name) },
                                onCopyPath = { viewModel.copyPath(item.name) },
                                onDownload = { viewModel.downloadItem(item.name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        itemToRename?.let { item ->
            RenameDialog(
                item = item,
                onDismiss = { itemToRename = null },
                onConfirm = { newName ->
                    viewModel.renameItem(item.name, newName)
                    itemToRename = null
                }
            )
        }

        itemToMove?.let { item ->
            MoveDialog(
                item = item,
                onDismiss = { itemToMove = null },
                onConfirm = { newPath ->
                    viewModel.moveItem(item.name, newPath)
                    itemToMove = null
                }
            )
        }

        itemToDelete?.let { item ->
            DeleteConfirmationDialog(
                item = item,
                onDismiss = { itemToDelete = null },
                onConfirm = {
                    viewModel.deleteItem(item.name)
                    itemToDelete = null
                }
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    item: StorageItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (item.isFolder) "Folder" else "File"}") },
        text = { Text("Are you sure you want to delete '${item.name}'? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "root",
            modifier = Modifier.clickable { onPathClick("") },
            color = if (currentPath.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        
        if (currentPath.isNotEmpty()) {
            val parts = currentPath.split("/")
            var accumulatedPath = ""
            parts.forEachIndexed { index, part ->
                accumulatedPath = if (accumulatedPath.isEmpty()) part else "$accumulatedPath/$part"
                val pathSnapshot = accumulatedPath
                
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = part,
                    modifier = Modifier.clickable { onPathClick(pathSnapshot) },
                    color = if (index == parts.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun StorageItemRow(
    item: StorageItem,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onGetUrl: () -> Unit,
    onCopyPath: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                isImage(extension) -> Icons.Default.Image
                isVideo(extension) -> Icons.Default.VideoLibrary
                isPdf(extension) -> Icons.Default.PictureAsPdf
                else -> Icons.Default.InsertDriveFile
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (item.isFolder) {
                        DropdownMenuItem(
                            text = { Text("Copy path") },
                            onClick = { onCopyPath(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { onRename(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = { onMove(); showMenu = false },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Get URL") },
                            onClick = { onGetUrl(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { onRename(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = { onMove(); showMenu = false },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Download") },
                            onClick = { onDownload(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(
    item: StorageItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val nameWithoutExtension = item.name.substringBeforeLast(".")
    
    var textFieldValue by remember {
        val initialText = item.name
        val selectionEnd = if (item.isFolder) initialText.length else nameWithoutExtension.length
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = androidx.compose.ui.text.TextRange(0, selectionEnd)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Rename ${if (item.isFolder) "Folder" else "File"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = { Text("New Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(textFieldValue.text) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rename")
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun MoveDialog(
    item: StorageItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPath by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Move ${if (item.isFolder) "Folder" else "File"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Enter new path in current bucket. Leave empty for root.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = newPath,
                onValueChange = { newPath = it },
                label = { Text("New Path") },
                placeholder = { Text("path/to/folder") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(newPath) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Move")
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

private fun isImage(extension: String) = extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private fun isVideo(extension: String) = extension in listOf("mp4", "mov", "avi", "mkv", "webm")
private fun isPdf(extension: String) = extension == "pdf"
