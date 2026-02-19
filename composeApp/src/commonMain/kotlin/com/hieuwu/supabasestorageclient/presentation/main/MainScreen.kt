package com.hieuwu.supabasestorageclient.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.presentation.buckets.BucketsScreen
import com.hieuwu.supabasestorageclient.presentation.settings.SettingsScreen
import com.hieuwu.supabasestorageclient.presentation.starred.StarredScreen
import kotlinx.coroutines.launch

enum class MainTab {
    Buckets, Starred, Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToBucket: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MainTab.Buckets) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Supabase Storage",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAbout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedTab.name, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Buckets") },
                        label = { Text("Buckets") },
                        selected = selectedTab == MainTab.Buckets,
                        onClick = { selectedTab = MainTab.Buckets }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = "Starred") },
                        label = { Text("Starred") },
                        selected = selectedTab == MainTab.Starred,
                        onClick = { selectedTab = MainTab.Starred }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = selectedTab == MainTab.Settings,
                        onClick = { selectedTab = MainTab.Settings }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    MainTab.Buckets -> BucketsScreen(
                        onBucketClick = onNavigateToBucket
                    )
                    MainTab.Starred -> StarredScreen()
                    MainTab.Settings -> SettingsScreen()
                }
            }
        }
    }
}
