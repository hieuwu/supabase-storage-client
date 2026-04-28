package com.hieuwu.supabasestorageclient.presentation.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
import com.hieuwu.supabasestorageclient.presentation.components.FileInfoDialog
import com.hieuwu.supabasestorageclient.core.formatDate
import com.hieuwu.supabasestorageclient.presentation.fileicons.FileIconUtils.getFileIcon
import com.hieuwu.supabasestorageclient.presentation.formatters.formatDownloadProgress
import com.hieuwu.supabasestorageclient.presentation.formatters.formatStatus
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val itemToDelete by viewModel.showDeleteConfirmationDialog
    val itemToCancel by viewModel.showCancelConfirmationDialog
    val itemToShowInfo by viewModel.showFileInfoDialog

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
                        onCancel = { viewModel.confirmCancel(item) },
                        onDelete = { viewModel.confirmDelete(item) },
                        onShowInfo = { viewModel.showFileInfo(item) }
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

        itemToCancel?.let { item ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissCancelConfirmation() },
                title = { Text("Cancel Download") },
                text = { Text("Are you sure you want to cancel downloading '${item.fileName}'?") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.cancelConfirmed(item.id) }
                    ) {
                        Text("Yes, cancel it")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissCancelConfirmation() }
                    ) {
                        Text("No")
                    }
                }
            )
        }

        itemToShowInfo?.let { item ->
            FileInfoDialog(
                fileName = item.fileName,
                icon = getFileIcon(item.fileName),
                status = item.formatStatus(),
                fromPath = item.from,
                toPath = item.destinationPath,
                size = item.formatDownloadProgress(),
                date = item.downloadedTime?.let { formatDate(it) },
                onDismissRequest = { viewModel.hideFileInfo() }
            )
        }
    }
}

@Composable
fun DownloadItemRow(
    item: DownloadItem,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onShowInfo() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getFileIcon(item.fileName),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
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
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("From: ")
                            }
                            append(item.from)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("To: ")
                            }
                            append(item.destinationPath)
                        },
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
                            text = item.formatStatus() ,
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
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
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
                            if (item.status == DownloadStatus.Downloading) {
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
            
            if (item.status == DownloadStatus.Downloading) {
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
                        text = item.formatDownloadProgress(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}