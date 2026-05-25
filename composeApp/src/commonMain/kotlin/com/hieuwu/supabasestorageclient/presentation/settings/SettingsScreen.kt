package com.hieuwu.supabasestorageclient.presentation.settings

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.domain.model.AppTheme
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    val content = uiState as? SettingsUiState.Content

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

    var pickingForSession by remember { mutableStateOf(false) }
    val directoryPickerLauncher = rememberDirectoryPickerLauncher { platformDirectory ->
        if (platformDirectory != null) {
            if (pickingForSession) {
                viewModel.updateSessionDownloadDirectory(platformDirectory.path)
            } else {
                viewModel.updateDefaultDownloadDirectory(platformDirectory.path)
            }
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
            label = "SettingsStateTransition"
        ) { state ->
            when (state) {
                is SettingsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SettingsUiState.Content -> {
                    val settings = state.settings
                    var limitInput by remember { mutableStateOf(settings.fileSizeLimit.toString()) }

                    LaunchedEffect(settings.fileSizeLimit) {
                        val currentParsed = limitInput.toLongOrNull() ?: 0L
                        if (currentParsed != settings.fileSizeLimit) {
                            limitInput = settings.fileSizeLimit.toString()
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "File Operations",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Default File Size Limit",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = limitInput,
                                            onValueChange = { newValue ->
                                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                                    limitInput = newValue
                                                    val parsedLimit = newValue.toLongOrNull() ?: 0L
                                                    viewModel.updateFileSizeLimit(parsedLimit)
                                                }
                                            },
                                            label = { Text("Limit") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )

                                        var unitExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = settings.fileSizeUnit.name.lowercase().replaceFirstChar { it.uppercase() },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Unit") },
                                                trailingIcon = {
                                                    IconButton(onClick = { unitExpanded = true }) {
                                                        Icon(
                                                            if (unitExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = null
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            DropdownMenu(
                                                expanded = unitExpanded,
                                                onDismissRequest = { unitExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.4f)
                                            ) {
                                                SizeUnit.entries.forEach { unit ->
                                                    DropdownMenuItem(
                                                        text = { Text(unit.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                        onClick = {
                                                            viewModel.updateFileUnit(unit)
                                                            unitExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Download Settings",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column {
                                    SettingsRow(
                                        title = "Ask download path",
                                        subtitle = settings.askDownloadPathConfig.label,
                                        icon = Icons.Default.HelpOutline,
                                        onClick = { }
                                    ) {
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { expanded = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Change Ask Download Path")
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                AskDownloadPathConfig.entries.forEach { config ->
                                                    DropdownMenuItem(
                                                        text = { Text(config.label) },
                                                        onClick = {
                                                            viewModel.updateAskDownloadPathConfig(config)
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    val isOnceWhenOpen = settings.askDownloadPathConfig == AskDownloadPathConfig.ONCE_WHEN_APP_OPEN
                                    val displayPath = if (isOnceWhenOpen) {
                                        settings.sessionDownloadDirectory ?: settings.defaultDownloadDirectory
                                    } else {
                                        settings.defaultDownloadDirectory
                                    }

                                    SettingsRow(
                                        title = if (isOnceWhenOpen) "Session download directory" else "Default download directory",
                                        subtitle = buildString {
                                            append(displayPath ?: "Not set")
                                            if (isOnceWhenOpen) {
                                                append("\n(Will be reset next time you open the app)")
                                            }
                                        },
                                        icon = if (isOnceWhenOpen) Icons.Default.FolderSpecial else Icons.Default.Folder,
                                        onClick = { },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                pickingForSession = isOnceWhenOpen
                                                directoryPickerLauncher.launch()
                                            }) {
                                                Icon(Icons.Default.FolderOpen, contentDescription = "Select Directory")
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column {
                                    SettingsRow(
                                        title = "Default View Mode",
                                        subtitle = settings.viewMode.label,
                                        icon = if (settings.viewMode == ViewMode.GRID) Icons.Default.GridView else Icons.Default.List,
                                        onClick = { }
                                    ) {
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { expanded = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Change View Mode")
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                ViewMode.entries.forEach { mode ->
                                                    DropdownMenuItem(
                                                        text = { Text(mode.label) },
                                                        onClick = {
                                                            viewModel.updateViewMode(mode)
                                                            expanded = false
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                if (mode == ViewMode.GRID) Icons.Default.GridView else Icons.Default.List,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    SettingsRow(
                                        title = "Theme",
                                        subtitle = settings.theme.label,
                                        icon = when (settings.theme) {
                                            AppTheme.LIGHT -> Icons.Default.LightMode
                                            AppTheme.DARK -> Icons.Default.DarkMode
                                            else -> Icons.Default.BrightnessAuto
                                        },
                                        onClick = { }
                                    ) {
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { expanded = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Change Theme")
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                AppTheme.entries.forEach { theme ->
                                                    DropdownMenuItem(
                                                        text = { Text(theme.label) },
                                                        onClick = {
                                                            viewModel.updateTheme(theme)
                                                            expanded = false
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                when (theme) {
                                                                    AppTheme.LIGHT -> Icons.Default.LightMode
                                                                    AppTheme.DARK -> Icons.Default.DarkMode
                                                                    AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                                                                },
                                                                contentDescription = null
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!state.isPro) {
                            item {
                                Text(
                                    text = "Subscription",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    SettingsRow(
                                        title = "Restore Purchases",
                                        subtitle = "Already purchased on another device?",
                                        icon = Icons.Default.Restore,
                                        onClick = { viewModel.onRestorePurchases() },
                                        trailingContent = {
                                            if (state.isRestoring) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Legal",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column {
                                    SettingsRow(
                                        title = "Terms of Service",
                                        subtitle = "Read our terms and conditions",
                                        icon = Icons.Default.Description,
                                        onClick = { uriHandler.openUri("https://hieuwu.github.io/supabuckt-landing/terms-of-service.html") }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    SettingsRow(
                                        title = "Privacy Policy",
                                        subtitle = "Read our privacy policy",
                                        icon = Icons.Default.Lock,
                                        onClick = { uriHandler.openUri("https://hieuwu.github.io/supabuckt-landing/privacy-policy.html") }
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

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    trailingContent: @Composable () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailingContent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        )
    )
}
