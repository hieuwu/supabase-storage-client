package com.hieuwu.supabasestorageclient.presentation.main

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.presentation.buckets.BucketsScreen
import com.hieuwu.supabasestorageclient.presentation.components.CredentialAvatar
import com.hieuwu.supabasestorageclient.presentation.downloads.DownloadsScreen
import com.hieuwu.supabasestorageclient.presentation.filebrowser.BucketScreen
import com.hieuwu.supabasestorageclient.presentation.fileview.FileViewScreen
import com.hieuwu.supabasestorageclient.presentation.navigation.Screen
import com.hieuwu.supabasestorageclient.presentation.settings.SettingsScreen
import com.hieuwu.supabasestorageclient.presentation.starred.StarredScreen
import com.hieuwu.supabasestorageclient.presentation.uploads.UploadScreen
import com.hieuwu.supabasestorageclient.presentation.components.PremiumBadge
import com.hieuwu.supabasestorageclient.presentation.components.UpgradeBadge
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    rootNavController: NavHostController,
    viewModel: MainViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberFilePickerLauncher { platformFile ->
        if (platformFile != null) {
            viewModel.onUploadFileSelected(platformFile)
        }
    }

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
                if (uiState.isPremium) {
                    PremiumBadge()
                } else {
                    UpgradeBadge(
                        onUpgradeClick = {
                            scope.launch { drawerState.close() }
                            viewModel.onUpgradeClick()
                        }
                    )
                }
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
                        var showActions by remember { mutableStateOf(false) }
                        FloatingActionButton(
                            onClick = { showActions = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }

                        if (showActions) {
                            ModalBottomSheet(
                                onDismissRequest = { showActions = false },
                                sheetState = rememberModalBottomSheetState()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .padding(bottom = 32.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        "Actions",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = {
                                            showActions = false
                                            viewModel.onNewFolderClick()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("New Folder")
                                    }
                                    Button(
                                        onClick = {
                                            filePickerLauncher.launch()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Upload File")
                                    }
                                }
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
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                    startDestination = Screen.BucketsTab.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { fadeOut(animationSpec = tween(300)) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    composable(Screen.BucketsTab.route) {
                        BucketsScreen(
                            onBucketClick = { bucketId ->
                                navController.navigate(Screen.Bucket.createRoute(bucketId))
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@composable
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
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@composable
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
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@composable
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
                            onBack = { navController.popBackStack() },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@composable
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
                            CredentialAvatar(name = credential.name)
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
        PremiumLoadingOverlay(message = "Setting up...")
    }
}

@Composable
private fun PremiumLoadingOverlay(message: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .blur(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Blur content
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotation
                        },
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
