package com.hieuwu.supabasestorageclient.presentation.uploads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.util.format
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UploadScreen(
    viewModel: UploadViewModel = koinViewModel()
) {
    val uploads by viewModel.uploads.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uploads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No uploads yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uploads, key = { it.id }) { item ->
                    UploadItemRow(
                        item = item,
                        onCancel = { viewModel.cancelUpload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun UploadItemRow(
    item: UploadItem,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    tint = MaterialTheme.colorScheme.secondary
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
                        text = formatStatus(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel/Remove")
                }
            }
            
            if (item.status == UploadStatus.Uploading) {
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
                        text = formatSize(item.uploadedSize, item.totalSize),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun getFileIcon(fileName: String): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "pdf" -> Icons.Default.Description
        "jpg", "jpeg", "png", "gif" -> Icons.Default.Image
        "mp4", "mov", "avi" -> Icons.Default.Movie
        "mp3", "wav" -> Icons.Default.MusicNote
        else -> Icons.Default.InsertDriveFile
    }
}

private fun formatStatus(item: UploadItem): String {
    return when (item.status) {
        UploadStatus.Uploading -> "Uploading..."
        UploadStatus.Paused -> "Paused"
        UploadStatus.Completed -> "Completed"
        UploadStatus.Error -> "Error"
        UploadStatus.Cancelled -> "Cancelled"
    }
}

private fun formatSize(uploaded: Long, total: Long): String {
    if (total <= 0) return formatBytes(uploaded)
    return "${formatBytes(uploaded)} / ${formatBytes(total)}"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.format(1)} KB"
    val mb = kb / 1024.0
    return "${mb.format(1)} MB"
}
