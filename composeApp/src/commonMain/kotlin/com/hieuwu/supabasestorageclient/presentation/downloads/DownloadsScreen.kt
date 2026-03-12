package com.hieuwu.supabasestorageclient.presentation.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
import com.hieuwu.supabasestorageclient.util.format
import com.hieuwu.supabasestorageclient.util.formatDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val itemToDelete by viewModel.showDeleteConfirmationDialog

    Box(modifier = Modifier.fillMaxSize()) {
        if (downloads.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Download,
                title = "No Downloads Yet",
                subtitle = "Your downloaded files will appear here."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadItemRow(
                        item = item,
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item.id) },
                        onCancel = { viewModel.cancelDownload(item.id) },
                        onDelete = { viewModel.confirmDelete(item) },
                        onOpenFile = { viewModel.openFile(item) },
                        onOpenDirectory = { viewModel.openDirectory(item) }
                    )
                }
            }
        }

        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirmation() },
                title = { Text("Delete Download") },
                text = { Text("Are you sure you want to delete '${item.fileName}' from your download list?") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteDownload(item.id) }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteConfirmation() }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DownloadItemRow(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenDirectory: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenFile() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getFileIcon(item.fileName),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clickable { onOpenDirectory() },
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "from: ${item.from}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatStatus(item),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (item.status == DownloadStatus.Completed && item.downloadedTime != null) {
                            Text(
                                text = formatDate(item.downloadedTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                
                Row {
                    if (item.status == DownloadStatus.Downloading) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }
                    } else if (item.status == DownloadStatus.Paused) {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                    }
                    
                    if (item.status == DownloadStatus.Completed) {
                        IconButton(onClick = onOpenDirectory) {
                            Icon(Icons.Default.Folder, contentDescription = "Open Directory")
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (item.status == DownloadStatus.Downloading || item.status == DownloadStatus.Paused) {
                                DropdownMenuItem(
                                    text = { Text("Cancel") },
                                    onClick = {
                                        onCancel()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
            
            if (item.status == DownloadStatus.Downloading || item.status == DownloadStatus.Paused) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatSize(item.downloadedSize, item.totalSize),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

fun getFileIcon(fileName: String): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "pdf" -> Icons.Default.Description
        "jpg", "jpeg", "png", "gif" -> Icons.Default.Image
        "mp4", "mov", "avi" -> Icons.Default.Movie
        "mp3", "wav" -> Icons.Default.MusicNote
        else -> Icons.Default.InsertDriveFile
    }
}

fun formatStatus(item: DownloadItem): String {
    return when (item.status) {
        DownloadStatus.Downloading -> "Downloading..."
        DownloadStatus.Paused -> "Paused"
        DownloadStatus.Completed -> "Completed"
        DownloadStatus.Error -> "Error"
        DownloadStatus.Cancelled -> "Cancelled"
    }
}

fun formatSize(downloaded: Long, total: Long): String {
    if (total <= 0) return formatBytes(downloaded)
    return "${formatBytes(downloaded)} / ${formatBytes(total)}"
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.format(1)} KB"
    val mb = kb / 1024.0
    return "${mb.format(1)} MB"
}
