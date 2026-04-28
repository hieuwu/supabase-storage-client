package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.presentation.components.EmptyState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BucketsScreen(
    onBucketClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: BucketsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val content = uiState as? BucketsUiState.Content

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
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "BucketsStateTransition"
        ) { state ->
            when (state) {
                is BucketsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                is BucketsUiState.Error -> {
                    EmptyState(
                        icon = Icons.Default.Storage,
                        title = "Error loading buckets",
                        subtitle = state.message
                    )
                }
                is BucketsUiState.Content -> {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.refreshBuckets() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.buckets.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.Storage,
                                title = "No buckets found",
                                subtitle = "You don't have any buckets yet. Create one to start storing files.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            AnimatedContent(
                                targetState = state.viewMode,
                                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                                label = "BucketsViewModeTransition"
                            ) { targetViewMode ->
                                if (targetViewMode == ViewMode.LIST) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(state.buckets) { bucket ->
                                            BucketListItem(
                                                bucket = bucket,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                onClick = { onBucketClick(bucket.id) },
                                                onToggleStar = { viewModel.toggleStar(bucket) },
                                                onEmptyClick = { viewModel.onEmptyBucketClick(bucket) },
                                                onDeleteClick = { viewModel.onDeleteBucketClick(bucket) }
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(160.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(state.buckets) { bucket ->
                                            BucketGridItem(
                                                bucket = bucket,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                onClick = { onBucketClick(bucket.id) },
                                                onToggleStar = { viewModel.toggleStar(bucket) },
                                                onEmptyClick = { viewModel.onEmptyBucketClick(bucket) },
                                                onDeleteClick = { viewModel.onDeleteBucketClick(bucket) }
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

    content?.let { state ->
        if (state.showEmptyConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialogs() },
                title = { Text("Empty Bucket") },
                text = { Text("Are you sure you want to empty the bucket '${state.selectedBucket?.name}'? All files will be deleted.") },
                confirmButton = {
                    Button(onClick = { viewModel.confirmEmptyBucket() }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialogs() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialogs() },
                title = { Text("Delete Bucket") },
                text = { Text("Are you sure you want to delete the bucket '${state.selectedBucket?.name}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeleteBucket() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialogs() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BucketListItem(
    bucket: Bucket,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onEmptyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = {
            with(sharedTransitionScope) {
                Text(
                    text = bucket.name,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${bucket.name}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        },
        supportingContent = {
            if (bucket.public) {
                Text("Public", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Private")
            }
        },
        leadingContent = {
            with(sharedTransitionScope) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "icon-${bucket.name}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleStar) {
                    Icon(
                        if (bucket.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = if (bucket.isStarred) "Unstar" else "Star",
                        tint = if (bucket.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Update bucket") },
                            onClick = {
                                showMenu = false
                                // Leave update bucket for now
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Empty bucket") },
                            onClick = {
                                showMenu = false
                                onEmptyClick()
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete bucket") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BucketGridItem(
    bucket: Bucket,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onEmptyClick: () -> Unit,
    onDeleteClick: () -> Unit
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.align(Alignment.TopStart).size(24.dp)
                ) {
                    Icon(
                        if (bucket.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (bucket.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Empty bucket") },
                            onClick = {
                                showMenu = false
                                onEmptyClick()
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete bucket") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }

            with(sharedTransitionScope) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(vertical = 8.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "icon-${bucket.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }

            with(sharedTransitionScope) {
                Text(
                    text = bucket.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "text-${bucket.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
            
            if (bucket.public) {
                Text(
                    "Public",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
