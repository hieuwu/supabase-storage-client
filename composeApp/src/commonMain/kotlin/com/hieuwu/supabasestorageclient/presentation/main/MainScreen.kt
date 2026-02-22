package com.hieuwu.supabasestorageclient.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.hieuwu.supabasestorageclient.presentation.filebrowser.BucketScreen
import com.hieuwu.supabasestorageclient.presentation.buckets.BucketsScreen
import com.hieuwu.supabasestorageclient.presentation.downloads.DownloadsScreen
import com.hieuwu.supabasestorageclient.presentation.fileview.FileViewScreen
import com.hieuwu.supabasestorageclient.presentation.navigation.Screen
import com.hieuwu.supabasestorageclient.presentation.settings.SettingsScreen
import com.hieuwu.supabasestorageclient.presentation.starred.StarredScreen
import com.hieuwu.supabasestorageclient.presentation.uploads.UploadScreen
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    rootNavController: NavHostController,
    viewModel: MainViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                        rootNavController.navigate(Screen.About.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when {
                            currentRoute?.startsWith("buckets-tab") == true -> "Buckets"
                            currentRoute?.startsWith("starred-tab") == true -> "Starred"
                            currentRoute?.startsWith("downloads-tab") == true -> "Downloads"
                            currentRoute?.startsWith("uploads-tab") == true -> "Uploads"
                            currentRoute?.startsWith("settings-tab") == true -> "Settings"
                            currentRoute?.startsWith("bucket") == true -> "Browse"
                            currentRoute?.startsWith("file-view") == true -> "File"
                            else -> "Supabase"
                        }
                        Text(title, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        val isSubScreen = (currentRoute?.startsWith("bucket") == true && currentRoute != Screen.BucketsTab.route) || 
                                          currentRoute?.startsWith("file-view") == true
                        if (isSubScreen) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { rootNavController.navigate(Screen.Search.route) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                when {
                    currentRoute == Screen.BucketsTab.route -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.onCreateBucketClick() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Create bucket") }
                        )
                    }
                    currentRoute?.startsWith("bucket") == true -> {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FloatingActionButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New Folder") },
                                    onClick = {
                                        expanded = false
                                        viewModel.onNewFolderClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Upload File") },
                                    onClick = {
                                        expanded = false
                                        viewModel.onUploadFileClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Buckets") },
                        label = { Text("Buckets") },
                        selected = currentRoute == Screen.BucketsTab.route || currentRoute?.startsWith("bucket") == true || currentRoute?.startsWith("file-view") == true,
                        onClick = {
                            navController.navigate(Screen.BucketsTab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = "Starred") },
                        label = { Text("Starred") },
                        selected = currentRoute == Screen.StarredTab.route,
                        onClick = {
                            navController.navigate(Screen.StarredTab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                        label = { Text("Downloads") },
                        selected = currentRoute == Screen.DownloadsTab.route,
                        onClick = {
                            navController.navigate(Screen.DownloadsTab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Upload, contentDescription = "Uploads") },
                        label = { Text("Uploads") },
                        selected = currentRoute == Screen.UploadsTab.route,
                        onClick = {
                            navController.navigate(Screen.UploadsTab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.SettingsTab.route,
                        onClick = {
                            navController.navigate(Screen.SettingsTab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.BucketsTab.route
                ) {
                    composable(Screen.BucketsTab.route) {
                        BucketsScreen(
                            onBucketClick = { bucketId ->
                                navController.navigate(Screen.Bucket.createRoute(bucketId))
                            }
                        )
                    }
                    composable(Screen.StarredTab.route) { StarredScreen() }
                    composable(Screen.DownloadsTab.route) { DownloadsScreen() }
                    composable(Screen.UploadsTab.route) { UploadScreen() }
                    composable(Screen.SettingsTab.route) { SettingsScreen() }

                    composable(
                        route = Screen.Bucket.route,
                        arguments = listOf(
                            navArgument("bucketId") { type = NavType.StringType },
                            navArgument("path") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) { backStackEntry ->
                        val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
                        val path = backStackEntry.arguments?.getString("path")
                        BucketScreen(
                            bucketId = bucketId,
                            path = path,
                            onBack = { navController.popBackStack() },
                            onNavigateToFolder = { newPath ->
                                navController.navigate(Screen.Bucket.createRoute(bucketId, newPath))
                            },
                            onNavigateToFile = { bId, fileName, p ->
                                navController.navigate(Screen.FileView.createRoute(bId, fileName, p))
                            }
                        )
                    }

                    composable(
                        route = Screen.FileView.route,
                        arguments = listOf(
                            navArgument("bucketId") { type = NavType.StringType },
                            navArgument("fileName") { type = NavType.StringType },
                            navArgument("path") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) { backStackEntry ->
                        val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
                        val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                        val path = backStackEntry.arguments?.getString("path")
                        FileViewScreen(
                            bucketId = bucketId,
                            fileName = fileName,
                            path = path,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isCreateBucketDialogVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissCreateBucketDialog() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Create Bucket",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.newBucketId,
                    onValueChange = { viewModel.onNewBucketIdChange(it) },
                    label = { Text("Bucket ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Public")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = uiState.isNewBucketPublic,
                        onCheckedChange = { viewModel.onNewBucketPublicToggle(it) }
                    )
                }
                OutlinedTextField(
                    value = uiState.newBucketFileSizeLimit,
                    onValueChange = { viewModel.onNewBucketFileSizeLimitChange(it) },
                    label = { Text("File Size Limit") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.newBucketFileSizeUnit.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        SizeUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.onNewBucketFileSizeUnitChange(unit)
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { viewModel.onConfirmCreateBucket() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create")
                }
                TextButton(
                    onClick = { viewModel.onDismissCreateBucketDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    if (uiState.isNewFolderDialogVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissNewFolderDialog() },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "New Folder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.newFolderName,
                    onValueChange = { viewModel.onNewFolderNameChange(it) },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.onCreateFolder() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create")
                }
                TextButton(
                    onClick = { viewModel.onDismissNewFolderDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }

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

    LaunchedEffect(viewModel.navigateToUploads) {
        viewModel.navigateToUploads.collect {
            navController.navigate(Screen.UploadsTab.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
