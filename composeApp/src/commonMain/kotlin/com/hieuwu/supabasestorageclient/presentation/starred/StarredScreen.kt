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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
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

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadStarredItems() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else if (uiState.items.isEmpty()) {
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
                    targetState = uiState.viewMode,
                    transitionSpec = {
                        fadeIn().togetherWith(fadeOut())
                    },
                    label = "StarredViewModeTransition"
                ) { targetViewMode ->
                    if (targetViewMode == ViewMode.LIST) {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.items) { item ->
                                StarredItemRow(
                                    item = item,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = {
                                        when {
                                            item.isBucket -> onNavigateToBucket(item.bucketId)
                                            item.isFolder -> onNavigateToFolder(
                                                item.bucketId,
                                                item.path ?: ""
                                            )
                                            else -> onNavigateToFile(
                                                item.bucketId,
                                                item.itemName,
                                                item.path?.substringBeforeLast("/", "")
                                            )
                                        }
                                    },
                                    onUnstar = { viewModel.unstarItem(item.itemId) }
                                )
                                HorizontalDivider()
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
                            items(uiState.items) { item ->
                                StarredItemGrid(
                                    item = item,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = {
                                        when {
                                            item.isBucket -> onNavigateToBucket(item.bucketId)
                                            item.isFolder -> onNavigateToFolder(
                                                item.bucketId,
                                                item.path ?: ""
                                            )
                                            else -> onNavigateToFile(
                                                item.bucketId,
                                                item.itemName,
                                                item.path?.substringBeforeLast("/", "")
                                            )
                                        }
                                    },
                                    onUnstar = { viewModel.unstarItem(item.itemId) }
                                )
                            }
                        }
                    }
                }
           }
        }
    }
    }

    if (uiState.showClearAllConfirmation) {
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
    ListItem(
        headlineContent = {
            with(sharedTransitionScope) {
                Text(
                    item.itemName,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${item.itemName}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        },
        supportingContent = {
            val typeStr = when {
                item.isBucket -> "Bucket"
                item.isFolder -> "Folder"
                else -> "File"
            }
            Text("$typeStr • ${item.bucketId}${if (item.path != null) "/${item.path}" else ""}")
        },
        leadingContent = {
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
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "icon-${item.itemName}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onUnstar) {
                    Icon(Icons.Default.Star, contentDescription = "Unstar", tint = MaterialTheme.colorScheme.primary)
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
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
                            rememberSharedContentState(key = "icon-${item.itemName}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }

            with(sharedTransitionScope) {
                Text(
                    text = item.itemName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${item.itemName}"),
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
