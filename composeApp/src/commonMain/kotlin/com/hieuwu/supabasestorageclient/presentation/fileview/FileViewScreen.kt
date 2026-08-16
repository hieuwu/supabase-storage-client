package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.core.FileUtils
import com.hieuwu.supabasestorageclient.core.formatDate
import com.hieuwu.supabasestorageclient.core.formatSize
import com.hieuwu.supabasestorageclient.platform.GifViewer
import com.hieuwu.supabasestorageclient.platform.PdfViewer
import com.hieuwu.supabasestorageclient.platform.VideoPlayer
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.path
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val content = uiState as? FileViewUiState.Content

    LaunchedEffect(content?.isDeleted) {
        if (content?.isDeleted == true) {
            kotlinx.coroutines.delay(1000)
            onBack()
        }
    }

    LaunchedEffect(content?.error, content?.successMessage) {
        content?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        content?.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val launcher = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { platformFile ->
        if (platformFile != null) {
            viewModel.startDownload(platformFile)
        } else {
            viewModel.onFileSaved()
        }
    }

    val directoryPickerLauncher = rememberDirectoryPickerLauncher { platformDirectory ->
        if (platformDirectory != null) {
            viewModel.onDirectoryPicked(platformDirectory.path)
        } else {
            viewModel.onDirectoryPickingCancelled()
        }
    }

    LaunchedEffect(content?.isPickingDirectory) {
        if (content?.isPickingDirectory == true) {
            directoryPickerLauncher.launch()
        }
    }

    LaunchedEffect(content?.isSavingFile, content?.itemToDownload) {
        if (content?.isSavingFile == true) {
            content.itemToDownload?.let { item ->
                val extension = item.name.substringAfterLast(".", "")
                val nameWithoutExtension = if (extension.isNotEmpty()) {
                    item.name.substringBeforeLast(".")
                } else {
                    item.name
                }
                
                launcher.launch(
                    suggestedName = nameWithoutExtension,
                    extension = extension
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "FileViewStateTransition"
        ) { state ->
            when (state) {
                is FileViewUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FileViewUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is FileViewUiState.Content -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            state.publicUrl?.let { url ->
                                FileViewerContent(url = url, fileName = fileName)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FileMetadataSection(
                            fileName = fileName,
                            extension = fileName.substringAfterLast(".", ""),
                            size = state.metadata?.size?.let { formatSize(it) } ?: "Unknown",
                            addedOn = formatDate(state.metadata?.createdAt),
                            lastModified = formatDate(state.metadata?.updatedAt),
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
                }
            }
        }
    }

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

    content?.let { state ->
        if (state.showDownloadPathOptionDialog) {
            DownloadPathOptionDialog(
                defaultPath = state.defaultDownloadPath,
                onDismiss = { viewModel.onCancelDownload() },
                onConfirmDefault = { viewModel.onSelectDefaultPath() },
                onConfirmCustom = { viewModel.onSelectCustomPath() }
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
        FileUtils.isGif(extension) -> {
            GifViewer(url = url)
        }

        FileUtils.isImage(extension) -> {
            ImageViewer(url = url)
        }

        FileUtils.isVideo(extension) -> {
            VideoPlayer(url = url)
        }

        FileUtils.isPdf(extension) -> {
            PdfViewer(url = url)
        }

        else -> {
            Text("No viewer available for this file type.") }
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
        FileUtils.isImage(extension) -> Icons.Default.Image
        FileUtils.isVideo(extension) -> Icons.Default.VideoLibrary
        FileUtils.isPdf(extension) -> Icons.Default.PictureAsPdf
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
@Composable
fun DownloadPathOptionDialog(
    defaultPath: String?,
    onDismiss: () -> Unit,
    onConfirmDefault: () -> Unit,
    onConfirmCustom: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Folder") },
        text = {
            Column {
                Text("How would you like to select the download folder for this session?")
                if (defaultPath != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Current default: $defaultPath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (defaultPath != null) {
                TextButton(onClick = onConfirmDefault) {
                    Text("Use Default")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onConfirmCustom) {
                Text("Pick Folder")
            }
        }
    )
}
