package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hieuwu.supabasestorageclient.util.formatDate
import com.hieuwu.supabasestorageclient.util.formatSize
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FileViewScreen(
    bucketId: String,
    fileName: String,
    path: String?,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: FileViewViewModel = koinViewModel(parameters = { parametersOf(bucketId, fileName, path) })
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            // Give some time for the success snackbar to be seen if it was triggered
            kotlinx.coroutines.delay(1000)
            onBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.error != null) {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                } else if (uiState.publicUrl != null) {
                    FileViewerContent(
                        url = uiState.publicUrl!!,
                        fileName = fileName
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FileMetadataSection(
                fileName = fileName,
                extension = fileName.substringAfterLast(".", ""),
                size = uiState.metadata?.size?.let { formatSize(it) } ?: "Unknown",
                addedOn = formatDate(uiState.metadata?.createdAt),
                lastModified = formatDate(uiState.metadata?.updatedAt),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )

            Spacer(modifier = Modifier.height(24.dp))

            FileActionsRow(
                onDownload = { viewModel.downloadFile() },
                onGetUrl = { viewModel.copyUrl() },
                onDelete = { showDeleteDialog = true }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete File") },
                text = { Text("Are you sure you want to delete '$fileName'? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteFile()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FileViewerContent(
    url: String,
    fileName: String
) {
    val extension = fileName.substringAfterLast(".", "").lowercase()
    when {
        isImage(extension) -> {
            ImageViewer(url = url)
        }
        isVideo(extension) -> {
            VideoPlayer(url = url)
        }
        isPdf(extension) -> {
            PdfViewer(url = url)
        }
        else -> {
            Text("No viewer available for this file type.")
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FileMetadataSection(
    fileName: String,
    extension: String,
    size: String,
    addedOn: String,
    lastModified: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val icon = when {
        isImage(extension) -> Icons.Default.Image
        isVideo(extension) -> Icons.Default.VideoLibrary
        isPdf(extension) -> Icons.Default.PictureAsPdf
        else -> Icons.Default.InsertDriveFile
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        with(sharedTransitionScope) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp)
                    .sharedElement(
                        rememberSharedContentState(key = "icon-$fileName"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            with(sharedTransitionScope) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-$fileName"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
            Text(
                text = "$extension - $size", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    MetadataItem(label = "Added on", value = addedOn)
    MetadataItem(label = "Last modified", value = lastModified)
}

@Composable
fun MetadataItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FileActionsRow(
    onDownload: () -> Unit,
    onGetUrl: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(icon = Icons.Default.Download, label = "Download", onClick = onDownload)
        ActionButton(icon = Icons.Default.ContentCopy, label = "Get URL", onClick = onGetUrl)
        ActionButton(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete, isDanger = true)
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun isImage(extension: String) = extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private fun isVideo(extension: String) = extension in listOf("mp4", "mov", "avi", "mkv", "webm")
private fun isPdf(extension: String) = extension == "pdf"
