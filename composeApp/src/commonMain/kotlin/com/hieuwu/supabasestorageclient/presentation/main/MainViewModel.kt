package com.hieuwu.supabasestorageclient.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import com.hieuwu.supabasestorageclient.domain.RefreshManager
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.observability.analytics.AnalyticsSources
import com.hieuwu.supabasestorageclient.observability.analytics.AppAnalytics
import com.hieuwu.supabasestorageclient.observability.analytics.SettingKeys
import com.hieuwu.supabasestorageclient.observability.analytics.StorageActions
import com.hieuwu.supabasestorageclient.observability.analytics.logBucketCreated
import com.hieuwu.supabasestorageclient.observability.analytics.logConnectionActivated
import com.hieuwu.supabasestorageclient.observability.analytics.logConnectionActivationFailed
import com.hieuwu.supabasestorageclient.observability.analytics.logConnectionRemoved
import com.hieuwu.supabasestorageclient.observability.analytics.logPaywallTriggered
import com.hieuwu.supabasestorageclient.observability.analytics.logRestoreCompleted
import com.hieuwu.supabasestorageclient.observability.analytics.logRestoreFailed
import com.hieuwu.supabasestorageclient.observability.analytics.logRestoreStarted
import com.hieuwu.supabasestorageclient.observability.analytics.logSettingChanged
import com.hieuwu.supabasestorageclient.observability.analytics.logStorageAction
import com.hieuwu.supabasestorageclient.observability.analytics.purchaseErrorReason
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val contextSelectionManager: ContextSelectionManager,
    private val createFolderUseCase: CreateFolderUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val createBucketUseCase: CreateBucketUseCase,
    private val observeProStatusUseCase: ObserveProStatusUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val updateBucketUseCase: UpdateBucketUseCase,
    private val observeCredentialsUseCase: ObserveCredentialsUseCase,
    private val getLastUsedCredentialIdUseCase: GetLastUsedCredentialIdUseCase,
    private val observeUserSettingsUseCase: GetUserSettingsUseCase,
    private val switchCredentialUseCase: SwitchCredentialUseCase,
    private val deleteCredentialUseCase: DeleteCredentialUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val triggerPaywallUseCase: TriggerPaywallUseCase,
    private val refreshManager: RefreshManager,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigateToUploads = MutableSharedFlow<Unit>()
    val navigateToUploads: SharedFlow<Unit> = _navigateToUploads.asSharedFlow()

    private val _navigateToBuckets = MutableSharedFlow<Unit>()
    val navigateToBuckets: SharedFlow<Unit> = _navigateToBuckets.asSharedFlow()

    init {
        observeData()
    }

    /** Reads the settings once; returns null (after logging) when they cannot be read. */
    private suspend fun currentSettings(): UserSettings? =
        runCatching { observeUserSettingsUseCase().first() }
            .onFailure { error -> logger.e(error) { "Failed to read user settings" } }
            .getOrNull()

    private fun observeData() {
        viewModelScope.launch {
            observeCredentialsUseCase()
                .catch { error -> logger.e(error) { "Failed to observe credentials" } }
                .collect { credentials ->
                    val lastUsedId = getLastUsedCredentialIdUseCase()
                    _uiState.update { it.copy(credentials = credentials, lastUsedId = lastUsedId) }
                }
        }

        viewModelScope.launch {
            observeUserSettingsUseCase()
                .catch { error -> logger.e(error) { "Failed to observe user settings" } }
                .collect { settings ->
                    _uiState.update { state ->
                        state.copy(
                            viewMode = settings.viewMode,
                            newBucketFileSizeLimit = if (state.newBucketFileSizeLimit.isEmpty()) settings.fileSizeLimit.toString() else state.newBucketFileSizeLimit,
                            newBucketFileSizeUnit = if (state.isNewBucketSizeLimitEnabled) settings.fileSizeUnit else state.newBucketFileSizeUnit
                        )
                    }
                }
        }

        viewModelScope.launch {
            observeProStatusUseCase()
                .catch { error -> logger.e(error) { "Failed to observe pro status" } }
                .collect { isPro ->
                    _uiState.update { it.copy(isPremium = isPro) }
                }
        }
    }

    fun onNewFolderClick() {
        _uiState.update { it.copy(isNewFolderDialogVisible = true) }
    }

    fun onDismissNewFolderDialog() {
        _uiState.update { it.copy(isNewFolderDialogVisible = false, newFolderName = "") }
    }

    fun onNewFolderNameChange(name: String) {
        _uiState.update { it.copy(newFolderName = name) }
    }

    fun onCreateBucketClick() {
        viewModelScope.launch {
            val settings = currentSettings() ?: return@launch
            _uiState.update {
                it.copy(
                    isCreateBucketDialogVisible = true,
                    isUpdateBucketMode = false,
                    isNewBucketSizeLimitEnabled = false,
                    newBucketFileSizeLimit = settings.fileSizeLimit.toString(),
                    newBucketFileSizeUnit = settings.fileSizeUnit
                )
            }
        }
    }

    fun onUpdateBucketClick(bucket: Bucket) {
        val (limit, unit) = bucket.fileSizeLimit?.let { convertBytesToUnit(it) } ?: Pair(
            0L,
            SizeUnit.MEGABYTES
        )
        _uiState.update {
            it.copy(
                isCreateBucketDialogVisible = true,
                isUpdateBucketMode = true,
                newBucketId = bucket.id,
                isNewBucketPublic = bucket.public,
                isNewBucketSizeLimitEnabled = bucket.fileSizeLimit != null,
                newBucketFileSizeLimit = if (bucket.fileSizeLimit != null) limit.toString() else it.newBucketFileSizeLimit,
                newBucketFileSizeUnit = if (bucket.fileSizeLimit != null) unit else it.newBucketFileSizeUnit
            )
        }
    }

    private fun convertBytesToUnit(bytes: Long): Pair<Long, SizeUnit> {
        return when {
            bytes % 1_073_741_824L == 0L -> Pair(bytes / 1_073_741_824L, SizeUnit.GIGABYTES)
            bytes % 1_048_576L == 0L -> Pair(bytes / 1_048_576L, SizeUnit.MEGABYTES)
            bytes % 1024L == 0L -> Pair(bytes / 1024L, SizeUnit.KILOBYTES)
            else -> Pair(bytes, SizeUnit.BYTES)
        }
    }

    fun onDismissCreateBucketDialog() {
        viewModelScope.launch {
            val settings = currentSettings() ?: return@launch
            _uiState.update {
                it.copy(
                    isCreateBucketDialogVisible = false,
                    isUpdateBucketMode = false,
                    newBucketId = "",
                    isNewBucketPublic = true,
                    isNewBucketSizeLimitEnabled = false,
                    newBucketFileSizeLimit = settings.fileSizeLimit.toString(),
                    newBucketFileSizeUnit = settings.fileSizeUnit
                )
            }
        }
    }

    fun onNewBucketIdChange(id: String) {
        _uiState.update { it.copy(newBucketId = id) }
    }

    fun onNewBucketPublicToggle(isPublic: Boolean) {
        _uiState.update { it.copy(isNewBucketPublic = isPublic) }
    }

    fun onNewBucketSizeLimitToggle(enabled: Boolean) {
        viewModelScope.launch {
            val settings = currentSettings() ?: return@launch
            _uiState.update {
                it.copy(
                    isNewBucketSizeLimitEnabled = enabled,
                    newBucketFileSizeLimit = if (enabled && it.newBucketFileSizeLimit.isEmpty())
                        settings.fileSizeLimit.toString() else it.newBucketFileSizeLimit,
                    newBucketFileSizeUnit = if (enabled)
                        settings.fileSizeUnit else it.newBucketFileSizeUnit
                )
            }
        }
    }

    fun onNewBucketFileSizeLimitChange(limit: String) {
        _uiState.update { it.copy(newBucketFileSizeLimit = limit) }
    }

    fun onNewBucketFileSizeUnitChange(unit: SizeUnit) {
        _uiState.update { it.copy(newBucketFileSizeUnit = unit) }
    }

    fun onConfirmCreateBucket() {
        val id = _uiState.value.newBucketId
        if (id.isBlank()) return

        val isSizeLimitEnabled = _uiState.value.isNewBucketSizeLimitEnabled
        val fileSizeLimit =
            if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeLimit.toLongOrNull() else null
        val isPublic = _uiState.value.isNewBucketPublic
        val unit = if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeUnit else null

        viewModelScope.launch {
            if (_uiState.value.isUpdateBucketMode) {
                val params = UpdateBucketUseCase.Params(
                    id = id,
                    public = isPublic,
                    fileSizeLimit = fileSizeLimit,
                    unit = unit
                )
                updateBucketUseCase(params).fold(
                    onSuccess = {
                        AppAnalytics.logStorageAction(StorageActions.UPDATE_BUCKET)
                        val settings = currentSettings()
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                isUpdateBucketMode = false,
                                newBucketId = "",
                                isNewBucketPublic = true,
                                isNewBucketSizeLimitEnabled = false,
                                newBucketFileSizeLimit = settings?.fileSizeLimit?.toString() ?: it.newBucketFileSizeLimit,
                                newBucketFileSizeUnit = settings?.fileSizeUnit ?: it.newBucketFileSizeUnit,
                                successMessage = "Bucket updated"
                            )
                        }
                        refreshManager.triggerRefreshBuckets()
                    },
                    onFailure = { error ->
                        AppAnalytics.logStorageAction(StorageActions.UPDATE_BUCKET, error)
                        logger.e(error) { "Failed to update bucket $id" }
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                isUpdateBucketMode = false,
                                error = error.message
                            )
                        }
                    }
                )
            } else {
                val params = CreateBucketUseCase.Params(
                    id = id,
                    public = isPublic,
                    fileSizeLimit = fileSizeLimit,
                    unit = unit
                )
                createBucketUseCase(params).fold(
                    onSuccess = {
                        AppAnalytics.logBucketCreated(
                            isPublic = isPublic,
                            hasSizeLimit = isSizeLimitEnabled,
                        )
                        val settings = currentSettings()
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                newBucketId = "",
                                isNewBucketPublic = true,
                                isNewBucketSizeLimitEnabled = false,
                                newBucketFileSizeLimit = settings?.fileSizeLimit?.toString() ?: it.newBucketFileSizeLimit,
                                newBucketFileSizeUnit = settings?.fileSizeUnit ?: it.newBucketFileSizeUnit,
                                successMessage = "Bucket created"
                            )
                        }
                        refreshManager.triggerRefreshBuckets()
                    },
                    onFailure = { error ->
                        AppAnalytics.logBucketCreated(
                            isPublic = isPublic,
                            hasSizeLimit = isSizeLimitEnabled,
                            error = error,
                        )
                        logger.e(error) { "Failed to create bucket $id" }
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                error = error.message
                            )
                        }
                    }
                )
            }
        }
    }

    fun onCreateFolder() {
        val folderName = _uiState.value.newFolderName
        if (folderName.isBlank()) return

        val context = contextSelectionManager.currentContext.value
        if (context == null) {
            _uiState.update { it.copy(error = "Please select a bucket first") }
            return
        }

        viewModelScope.launch {
            val fullPath = if (context.path.isEmpty()) folderName else "${context.path}/$folderName"
            val params = CreateFolderUseCase.Params(bucketId = context.bucketId, path = fullPath)
            createFolderUseCase(params).fold(
                onSuccess = {
                    AppAnalytics.logStorageAction(StorageActions.CREATE_FOLDER)
                    _uiState.update {
                        it.copy(
                            isNewFolderDialogVisible = false,
                            newFolderName = "",
                            successMessage = "Folder created"
                        )
                    }
                },
                onFailure = { error ->
                    AppAnalytics.logStorageAction(StorageActions.CREATE_FOLDER, error)
                    logger.e(error) { "Failed to create folder $fullPath in ${context.bucketId}" }
                    _uiState.update {
                        it.copy(
                            isNewFolderDialogVisible = false,
                            newFolderName = "",
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun onUploadFileSelected(platformFile: io.github.vinceglb.filekit.PlatformFile) {
        viewModelScope.launch {
            val context = contextSelectionManager.currentContext.value
            if (context == null) {
                _uiState.update { it.copy(error = "Please select a bucket first") }
                return@launch
            }

            // Reading the picked file can fail on its own (revoked permission, file deleted
            // between picking and reading), before any UploadItem exists to attach an error to.
            val fileName = runCatching { platformFile.name }.getOrElse { error ->
                logger.e(error) { "Failed to read name of the picked file" }
                _uiState.update { it.copy(error = "Could not read the selected file") }
                return@launch
            }
            val data = runCatching { platformFile.readBytes() }.getOrElse { error ->
                logger.e(error) { "Failed to read contents of $fileName" }
                _uiState.update { it.copy(error = "Could not read \"$fileName\": ${error.message}") }
                return@launch
            }
            val fullPath = if (context.path.isEmpty()) fileName else "${context.path}/$fileName"

            val params = UploadFileUseCase.Params(
                bucketId = context.bucketId,
                path = fullPath,
                fileName = fileName,
                data = data
            )
            uploadFileUseCase(params)
            _uiState.update { it.copy(successMessage = "Upload started", isUploading = true) }
            _navigateToUploads.emit(Unit)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, isUploading = false) }
    }

    fun onLogoutClick() {
        _uiState.update { it.copy(isCredentialsSheetVisible = true) }
    }

    fun onDismissCredentialsSheet() {
        _uiState.update { it.copy(isCredentialsSheetVisible = false) }
    }

    fun onCredentialClick(credential: Credential) {
        if (credential.id == _uiState.value.lastUsedId) return
        _uiState.update {
            it.copy(
                isCredentialsSheetVisible = false,
                showCredentialSwitchConfirmation = true,
                selectedCredentialForSwitch = credential
            )
        }
    }

    fun onDismissCredentialSwitchConfirmation() {
        _uiState.update {
            it.copy(
                showCredentialSwitchConfirmation = false,
                selectedCredentialForSwitch = null
            )
        }
    }

    fun onConfirmCredentialSwitch() {
        val credential = _uiState.value.selectedCredentialForSwitch ?: return
        _uiState.update {
            it.copy(
                showCredentialSwitchConfirmation = false,
                isSettingUpCredential = true
            )
        }
        viewModelScope.launch {
            switchCredentialUseCase(credential).fold(
                onSuccess = {
                    AppAnalytics.logConnectionActivated(
                        connectionCount = _uiState.value.credentials.size,
                        isFirst = false,
                    )
                    _navigateToBuckets.emit(Unit)
                    _uiState.update {
                        it.copy(
                            lastUsedId = credential.id,
                            isSettingUpCredential = false
                        )
                    }
                },
                onFailure = { error ->
                    AppAnalytics.logConnectionActivationFailed(error)
                    logger.e(error) { "Failed to switch to credential ${credential.id}" }
                    _uiState.update {
                        it.copy(
                            error = "Switch failed: ${error.message}",
                            isSettingUpCredential = false
                        )
                    }
                }
            )
        }
    }

    fun onRemoveCredential(id: String) {
        viewModelScope.launch {
            deleteCredentialUseCase(id).fold(
                onSuccess = {
                    AppAnalytics.logConnectionRemoved(
                        connectionCount = (_uiState.value.credentials.size - 1).coerceAtLeast(0),
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to remove credential $id" }
                    _uiState.update { it.copy(error = "Failed to remove credential: ${error.message}") }
                }
            )
            // If current was removed, the UseCase or Manager should handle clearing technically,
            // but for UI we might need to react if not already doing so via credentials stream.
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val settings = currentSettings() ?: return@launch
            val newViewMode =
                if (settings.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            updateUserSettingsUseCase(settings.copy(viewMode = newViewMode)).fold(
                onSuccess = {
                    AppAnalytics.logSettingChanged(SettingKeys.VIEW_MODE, newViewMode.name.lowercase())
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save view mode $newViewMode" }
                    _uiState.update { it.copy(error = "Could not save the view mode") }
                }
            )
        }
    }

    fun onUpgradeClick() {
        AppAnalytics.logPaywallTriggered(AnalyticsSources.UPGRADE_BUTTON)
        triggerPaywallUseCase()
    }

    fun onRestoreClick() {
        _uiState.update { it.copy(isRestoring = true) }
        AppAnalytics.logRestoreStarted(AnalyticsSources.MAIN)
        viewModelScope.launch {
            val result = restorePurchasesUseCase()
            _uiState.update { it.copy(isRestoring = false) }
            when (result) {
                is ApiResponse.Success -> {
                    // The repository refreshes pro status before returning, so this reads the
                    // outcome of the restore, not the state from before it.
                    AppAnalytics.logRestoreCompleted(
                        source = AnalyticsSources.MAIN,
                        isPro = observeProStatusUseCase().first(),
                    )
                    _uiState.update { it.copy(successMessage = "Restored successfully!") }
                }

                is ApiResponse.Error -> {
                    AppAnalytics.logRestoreFailed(
                        source = AnalyticsSources.MAIN,
                        reason = purchaseErrorReason(result.exception.message),
                    )
                    _uiState.update { it.copy(error = "Restore failed: ${result.exception.message}") }
                }

                is ApiResponse.Loading -> {}
            }
        }
    }
}
