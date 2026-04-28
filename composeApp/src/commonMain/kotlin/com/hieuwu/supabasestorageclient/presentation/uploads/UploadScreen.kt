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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.core.FileUtils
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
import com.hieuwu.supabasestorageclient.presentation.components.FileInfoDialog
import com.hieuwu.supabasestorageclient.core.format
import com.hieuwu.supabasestorageclient.core.formatDateTime
import com.hieuwu.supabasestorageclient.presentation.fileicons.FileIconUtils.getFileIcon
import com.hieuwu.supabasestorageclient.presentation.formatters.formatStatus
import com.hieuwu.supabasestorageclient.presentation.formatters.formatUploadProgress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UploadScreen(
    viewModel: UploadViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UploadUiState.Empty -> {
                EmptyState(
                    icon = Icons.Default.Upload,
                    title = "No Uploads Yet",
                    subtitle = "Your file uploads will appear here."
                )
            }

            is UploadUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.uploads, key = { it.id }) { item ->
                        UploadItemRow(
                            item = item,
                            onCancel = { viewModel.cancelUpload(item.id) },
                            onShowInfo = { viewModel.showFileInfo(item) }
                        )
                    }
                }

                state.selectedItem?.let { item ->
                    FileInfoDialog(
                        fileName = item.fileName,
                        icon = getFileIcon(item.fileName),
                        status = item.formatStatus(),
                        fromPath = item.from,
                        toPath = item.to,
                        size =  item.formatUploadProgress(),
                        date = item.uploadedTime?.let { formatDateTime(it) },
                        onDismissRequest = { viewModel.hideFileInfo() }
                    )
                }
            }
        }
    }
}

@Composable
fun UploadItemRow(
    item: UploadItem,
    onCancel: () -> Unit,
    onShowInfo: () -> Unit
) {
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
                            append(item.to)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.formatStatus(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = item.formatUploadProgress(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}