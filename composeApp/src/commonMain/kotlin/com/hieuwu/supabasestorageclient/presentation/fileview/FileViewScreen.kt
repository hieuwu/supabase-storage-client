package com.hieuwu.supabasestorageclient.presentation.fileview

import com.hieuwu.supabasestorageclient.presentation.fileview.FileViewViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    bucketId: String,
    fileName: String,
    path: String?,
    onBack: () -> Unit,
    viewModel: FileViewViewModel = koinViewModel(parameters = { parametersOf(bucketId, fileName, path) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
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
                size = uiState.metadata?.size?.let { "${it / 1024} KB" } ?: "Unknown",
                addedOn = uiState.metadata?.createdAt?.toString() ?: "Unknown",
                lastModified = uiState.metadata?.updatedAt?.toString() ?: "Unknown"
            )

            Spacer(modifier = Modifier.height(24.dp))

            FileActionsRow(
                onDownload = { viewModel.downloadFile() },
                onGetUrl = { viewModel.copyUrl() },
                onDelete = { viewModel.deleteFile() }
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

@Composable
fun FileMetadataSection(
    fileName: String,
    extension: String,
    size: String,
    addedOn: String,
    lastModified: String
) {
    val icon = when {
        isImage(extension) -> Icons.Default.Image
        isVideo(extension) -> Icons.Default.VideoLibrary
        isPdf(extension) -> Icons.Default.PictureAsPdf
        else -> Icons.Default.InsertDriveFile
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp).padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName, 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold,
            )
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
