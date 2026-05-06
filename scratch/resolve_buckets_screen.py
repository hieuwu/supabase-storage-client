
import os

filepath = 'composeApp/src/commonMain/kotlin/com/hieuwu/supabasestorageclient/presentation/buckets/BucketsScreen.kt'

with open(filepath, 'r') as f:
    lines = f.readlines()

start_index = -1
end_index = -1
for i, line in enumerate(lines):
    if '<<<<<<< HEAD' in line:
        start_index = i
    if start_index != -1 and '>>>>>>> main' in line:
        end_index = i
        break

if start_index != -1 and end_index != -1:
    # I want the merged content. 
    # Basically the HEAD branch content but with onUpdateClick added.
    merged_content = """                is BucketsUiState.Error -> {
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
                                                onUpdateClick = { onUpdateBucketClick(bucket) },
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
                                                onUpdateClick = { onUpdateBucketClick(bucket) },
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
"""
    new_lines = lines[:start_index] + [merged_content] + lines[end_index+1:]
    with open(filepath, 'w') as f:
        f.writelines(new_lines)
    print("Successfully updated BucketsScreen.kt")
else:
    print(f"Could not find function boundaries: {start_index}, {end_index}")
