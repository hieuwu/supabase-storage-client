@file:OptIn(ExperimentalMaterial3Api::class)

package com.hieuwu.supabasestorageclient.presentation.filebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
import com.hieuwu.supabasestorageclient.util.formatDate
import com.hieuwu.supabasestorageclient.util.formatSize
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
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refreshContents() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading && uiState.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else if (uiState.items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Folder,
                        title = "No files found",
                        subtitle = "This folder is empty. Upload some files to get started.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SharedTransitionLayout {
                        AnimatedContent(
                            targetState = uiState.viewMode,
                            transitionSpec = {
                                fadeIn().togetherWith(fadeOut())
                            },
                            label = "ViewModeTransition"
                        ) { targetViewMode ->
                            if (targetViewMode == ViewMode.LIST) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(uiState.items) { item ->
                                        StorageItemRow(
                                            item = item,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent,
                                            onClick = {
                                                if (item.isFolder) {
                                                    val nextPath =
                                                        if (path.isNullOrEmpty()) item.name else "$path/${item.name}"
                                                    onNavigateToFolder(nextPath)
                                                } else {
                                                    onNavigateToFile(bucketId, item.name, path)
                                                }
                                            },
                                            onRename = { itemToRename = item },
                                            onMove = { itemToMove = item },
                                            onDelete = { itemToDelete = item },
                                            onStar = { viewModel.toggleStar(item) },
                                            onGetUrl = { viewModel.getPublicUrl(item.name) },
                                            onCopyPath = { viewModel.copyPath(item.name) },
                                            onDownload = { viewModel.downloadItem(item.name) }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(120.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.items) { item ->
                                        StorageItemGrid(
                                            item = item,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent,
                                            onClick = {
                                                if (item.isFolder) {
                                                    val nextPath =
                                                        if (path.isNullOrEmpty()) item.name else "$path/${item.name}"
                                                    onNavigateToFolder(nextPath)
                                                } else {
                                                    onNavigateToFile(bucketId, item.name, path)
                                                }
                                            },
                                            onRename = { itemToRename = item },
                                            onMove = { itemToMove = item },
                                            onDelete = { itemToDelete = item },
                                            onStar = { viewModel.toggleStar(item) },
                                            onGetUrl = { viewModel.getPublicUrl(item.name) },
                                            onCopyPath = { viewModel.copyPath(item.name) },
                                            onDownload = { viewModel.downloadItem(item.name) }
                                        )
                                    }
                                }
                            }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StorageItemRow(
    item: StorageItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onStar: () -> Unit,
    onGetUrl: () -> Unit,
    onCopyPath: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            with(sharedTransitionScope) {
                Text(
                    text = item.name,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${item.name}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        },
        supportingContent = {
            if (!item.isFolder) {
                val sizeStr = item.size?.let { formatSize(it) } ?: ""
                val dateStr = formatDate(item.updatedAt)
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
            with(sharedTransitionScope) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "icon-${item.name}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onStar) {
                    Icon(
                        imageVector = if (item.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = if (item.isStarred) "Unstar" else "Star",
                        tint = if (item.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null
                                )
                            }
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
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null
                                )
                            }
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
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StorageItemGrid(
    item: StorageItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onStar: () -> Unit,
    onGetUrl: () -> Unit,
    onCopyPath: () -> Unit,
    onDownload: () -> Unit
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
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onStar,
                    modifier = Modifier.align(Alignment.TopStart).size(24.dp)
                ) {
                    Icon(
                        if (item.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (item.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                    if (item.isFolder) {
                        DropdownMenuItem(
                            text = { Text("Copy path") },
                            onClick = { onCopyPath(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { onRename(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = { onMove(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null
                                )
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Get URL") },
                            onClick = { onGetUrl(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { onRename(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = { onMove(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Download") },
                            onClick = { onDownload(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                    }
                }
            }

            val extension = item.name.substringAfterLast(".", "").lowercase()
            val icon = when {
                item.isFolder -> Icons.Default.Folder
                isImage(extension) -> Icons.Default.Image
                isVideo(extension) -> Icons.Default.VideoLibrary
                isPdf(extension) -> Icons.Default.PictureAsPdf
                else -> Icons.Default.InsertDriveFile
            }

            with(sharedTransitionScope) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(vertical = 8.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "icon-${item.name}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }

            with(sharedTransitionScope) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${item.name}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }

            if (!item.isFolder) {
                val sizeStr = item.size?.let { "${it / 1024} KB" } ?: ""
                Text(
                    text = sizeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
