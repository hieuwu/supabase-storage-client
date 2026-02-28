package com.hieuwu.supabasestorageclient.presentation.main

import androidx.compose.foundation.clickable
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
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
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
import com.hieuwu.supabasestorageclient.domain.model.Credential
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
                        val isSubScreen =
                            (currentRoute?.startsWith("bucket") == true && currentRoute != Screen.BucketsTab.route) ||
                                    currentRoute?.startsWith("file-view") == true
                        if (isSubScreen) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
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
                        val showViewSwitcher = currentRoute?.startsWith("buckets-tab") == true ||
                                currentRoute?.startsWith("starred-tab") == true ||
                                currentRoute?.startsWith("bucket") == true

                        if (showViewSwitcher) {
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Switch View"
                                )
                            }
                        }
                        IconButton(onClick = {
                            val bucketId =
                                if (currentRoute?.startsWith("bucket") == true && currentRoute != Screen.BucketsTab.route) {
                                    // Extract bucketId from route bucket/{bucketId}?path={path}
                                    navBackStackEntry?.savedStateHandle?.get<String>("bucketId") ?: ""
                                } else null
                            rootNavController.navigate(Screen.Search.createRoute(bucketId))
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { viewModel.onLogoutClick() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                when {
                    currentRoute == Screen.BucketsTab.route -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.onCreateBucketClick() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Create bucket") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    currentRoute?.startsWith("bucket") == true -> {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FloatingActionButton(
                                onClick = { expanded = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
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
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CreateNewFolder,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Upload File") },
                                    onClick = {
                                        expanded = false
                                        viewModel.onUploadFileClick()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.UploadFile,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Buckets") },
                        label = { Text("Buckets") },
                        selected = currentRoute == Screen.BucketsTab.route || currentRoute?.startsWith(
                            "bucket"
                        ) == true || currentRoute?.startsWith("file-view") == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
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
                    composable(Screen.StarredTab.route) { 
                        StarredScreen(
                            onNavigateToBucket = { bucketId ->
                                navController.navigate(Screen.Bucket.createRoute(bucketId))
                            },
                            onNavigateToFolder = { bucketId, path ->
                                navController.navigate(Screen.Bucket.createRoute(bucketId, path))
                            },
                            onNavigateToFile = { bId, fileName, p ->
                                navController.navigate(Screen.FileView.createRoute(bId, fileName, p))
                            }
                        ) 
                    }
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
                        val bucketId = backStackEntry.savedStateHandle.get<String>("bucketId") ?: ""
                        val path = backStackEntry.savedStateHandle.get<String>("path") ?: ""
                        BucketScreen(
                            bucketId = bucketId,
                            path = path,
                            onBack = { navController.popBackStack() },
                            onNavigateToFolder = { newPath ->
                                navController.navigate(Screen.Bucket.createRoute(bucketId, newPath))
                            },
                            onNavigateToFile = { bId, fileName, p ->
                                navController.navigate(
                                    Screen.FileView.createRoute(
                                        bId,
                                        fileName,
                                        p
                                    )
                                )
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
                        val bucketId = backStackEntry.savedStateHandle.get("bucketId") ?: ""
                        val fileName =  backStackEntry.savedStateHandle.get("fileName") ?: ""
                        val path = backStackEntry.savedStateHandle.get("path") ?: ""
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
                
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Size Limit")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = uiState.isNewBucketSizeLimitEnabled,
                        onCheckedChange = { viewModel.onNewBucketSizeLimitToggle(it) }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isNewBucketSizeLimitEnabled
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                value = uiState.newBucketFileSizeUnit.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
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
                                        text = {
                                            Text(
                                                unit.name.lowercase().replaceFirstChar { it.uppercase() })
                                        },
                                        onClick = {
                                            viewModel.onNewBucketFileSizeUnitChange(unit)
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
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

    LaunchedEffect(viewModel.navigateToBuckets) {
        viewModel.navigateToBuckets.collect {
            navController.navigate(Screen.BucketsTab.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    if (uiState.isCredentialsSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissCredentialsSheet() },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                uiState.credentials.forEach { credential ->
                    val isSelected = credential.id == uiState.lastUsedId
                    ListItem(
                        headlineContent = { Text(credential.name) },
                        supportingContent = { Text(credential.url) },
                        leadingContent = {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                            }
                        },
                        trailingContent = {
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Remove") },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.onRemoveCredential(credential.id)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            viewModel.onCredentialClick(credential)
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.onDismissCredentialsSheet()
                        rootNavController.navigate(Screen.Credentials.route)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Credential")
                }
            }
        }
    }

    if (uiState.showCredentialSwitchConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCredentialSwitchConfirmation() },
            title = { Text("Change Credentials") },
            text = { Text("Are you sure you want to change credentials to ${uiState.selectedCredentialForSwitch?.name}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmCredentialSwitch() }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissCredentialSwitchConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.isSettingUpCredential) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Setting up") },
            text = {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        )
    }
}
