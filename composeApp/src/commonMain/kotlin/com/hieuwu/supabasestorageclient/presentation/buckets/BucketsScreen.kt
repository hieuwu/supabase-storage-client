package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BucketsScreen(
    onBucketClick: (String) -> Unit,
    viewModel: BucketsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.buckets.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null && uiState.buckets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.loadBuckets() }) {
                    Text("Retry")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.buckets) { bucket ->
                    ListItem(
                        headlineContent = { Text(bucket.name) },
                        supportingContent = { Text(if (bucket.public) "Public" else "Private") },
                        leadingContent = {
                            Icon(Icons.Default.Storage, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onBucketClick(bucket.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

