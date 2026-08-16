package com.hieuwu.supabasestorageclient.presentation.starred

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.core.FileUtils
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
import com.hieuwu.supabasestorageclient.core.formatDateTime
import com.hieuwu.supabasestorageclient.presentation.fileicons.FileIconUtils.getFileIcon
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(
    onNavigateToBucket: (String) -> Unit,
    onNavigateToFolder: (String, String) -> Unit,
    onNavigateToFile: (String, String, String?) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: StarredViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val content = uiState as? StarredUiState.Content

    LaunchedEffect(content?.successMessage, content?.error) {
        content?.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        content?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.fillMaxSize()
    ) { padding ->
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            contentKey = { it::class },
            label = "StarredStateTransition"
        ) { state ->
            when (state) {
                is StarredUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                is StarredUiState.Error -> {
                    EmptyState(
                        icon = Icons.Default.Error,
                        title = "Error loading starred items",
                        subtitle = state.message
                    )
                }
                is StarredUiState.Content -> {
                    if (state.items.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.StarOutline,
                            title = "No Starred Items",
                            subtitle = "Your starred files and folders will appear here."
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.onClearAllClick() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Clear All")
                                }
                            }
                            AnimatedContent(
                                targetState = state.viewMode,
                                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                                label = "StarredViewModeTransition"
                            ) { targetViewMode ->
                                if (targetViewMode == ViewMode.LIST) {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(state.items) { item ->
                                            StarredItemRow(
                                                item = item,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                onClick = {
                                                    when {
                                                        item.isBucket -> onNavigateToBucket(item.bucketId)
                                                        item.isFolder -> onNavigateToFolder(item.bucketId, item.path ?: "")
                                                        else -> onNavigateToFile(item.bucketId, item.fileName, item.path?.substringBeforeLast("/", ""))
                                                    }
                                                },
                                                onUnstar = { viewModel.unstarItem(item.id) }
                                            )
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(140.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(state.items) { item ->
                                            StarredItemGrid(
                                                item = item,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                onClick = {
                                                    when {
                                                        item.isBucket -> onNavigateToBucket(item.bucketId)
                                                        item.isFolder -> onNavigateToFolder(item.bucketId, item.path ?: "")
                                                        else -> onNavigateToFile(item.bucketId, item.fileName, item.path?.substringBeforeLast("/", ""))
                                                    }
                                                },
                                                onUnstar = { viewModel.unstarItem(item.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (content?.showClearAllConfirmation == true) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAllConfirmation() },
            title = { Text("Clear All Starred") },
            text = { Text("Are you sure you want to remove all items from starred?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearAll() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearAllConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StarredItemRow(
    item: StarredItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onUnstar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when {
                    item.isBucket -> Icons.Default.Storage
                    item.isFolder -> Icons.Default.Folder
                    else -> getFileIcon(item.fileName)
                }

                with(sharedTransitionScope) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "icon-${item.fileName}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    with(sharedTransitionScope) {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "text-${item.fileName}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Bucket: ")
                            }
                            append(item.bucketId)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (item.path != null) {
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Path: ")
                                }
                                append(item.path)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Starred at ${formatDateTime(item.starredAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = onUnstar) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Unstar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StarredItemGrid(
    item: StarredItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onUnstar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onUnstar,
                    modifier = Modifier.align(Alignment.TopStart).size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Unstar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val icon = when {
                item.isBucket -> Icons.Default.Storage
                item.isFolder -> Icons.Default.Folder
                else -> Icons.Default.InsertDriveFile
            }

            with(sharedTransitionScope) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (item.isFolder || item.isBucket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(vertical = 8.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "icon-${item.fileName}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }

            with(sharedTransitionScope) {
                Text(
                    text = item.fileName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${item.fileName}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }

            val typeStr = when {
                item.isBucket -> "Bucket"
                item.isFolder -> "Folder"
                else -> "File"
            }
            Text(
                text = typeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
