package com.hieuwu.supabasestorageclient.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.usecase.CreateBucketUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.CreateFolderUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.UploadFileUseCase
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.usecase.UpdateBucketUseCase
import com.hieuwu.supabasestorageclient.domain.RefreshManager
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.hieuwu.supabasestorageclient.data.network.ApiResponse

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    private val contextSelectionManager: ContextSelectionManager,
    private val createFolderUseCase: CreateFolderUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val createBucketUseCase: CreateBucketUseCase,
    private val credentialRepository: CredentialRepository,
    private val settingsRepository: SettingsRepository,
    private val supabaseClientManager: SupabaseClientManager,
    private val purchaseRepository: PurchaseRepository,
    private val updateBucketUseCase: UpdateBucketUseCase,
    private val refreshManager: RefreshManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigateToUploads = MutableSharedFlow<Unit>()
    val navigateToUploads: SharedFlow<Unit> = _navigateToUploads.asSharedFlow()

    private val _navigateToBuckets = MutableSharedFlow<Unit>()
    val navigateToBuckets: SharedFlow<Unit> = _navigateToBuckets.asSharedFlow()

    private var currentSettings: UserSettings? = null

    init {
        observeCredentials()
        observeSettings()
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            purchaseRepository.isPro.collect { isPro ->
                _uiState.update { it.copy(isPremium = isPro) }
            }
        }
    }

    private fun observeCredentials() {
        viewModelScope.launch {
            credentialRepository.getCredentials().collect { credentials ->
                val lastUsedId = credentialRepository.getLastUsedId()
                _uiState.update { it.copy(credentials = credentials, lastUsedId = lastUsedId) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                currentSettings = settings
                _uiState.update { it.copy(viewMode = settings.viewMode) }
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
        _uiState.update { 
            it.copy(
                isCreateBucketDialogVisible = true,
                isUpdateBucketMode = false,
                isNewBucketSizeLimitEnabled = false,
                newBucketFileSizeLimit = currentSettings?.fileSizeLimit?.toString() ?: "",
                newBucketFileSizeUnit = currentSettings?.fileSizeUnit ?: SizeUnit.MEGABYTES
            )
        }
    }

    fun onUpdateBucketClick(bucket: Bucket) {
        val (limit, unit) = bucket.fileSizeLimit?.let { convertBytesToUnit(it) } ?: Pair(0L, SizeUnit.MEGABYTES)
        _uiState.update {
            it.copy(
                isCreateBucketDialogVisible = true,
                isUpdateBucketMode = true,
                newBucketId = bucket.id,
                isNewBucketPublic = bucket.public,
                isNewBucketSizeLimitEnabled = bucket.fileSizeLimit != null,
                newBucketFileSizeLimit = if (bucket.fileSizeLimit != null) limit.toString() else currentSettings?.fileSizeLimit?.toString() ?: "",
                newBucketFileSizeUnit = if (bucket.fileSizeLimit != null) unit else currentSettings?.fileSizeUnit ?: SizeUnit.MEGABYTES
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
        _uiState.update {
            it.copy(
                isCreateBucketDialogVisible = false,
                isUpdateBucketMode = false,
                newBucketId = "",
                isNewBucketPublic = true,
                isNewBucketSizeLimitEnabled = false,
                newBucketFileSizeLimit = currentSettings?.fileSizeLimit?.toString() ?: "",
                newBucketFileSizeUnit = currentSettings?.fileSizeUnit ?: SizeUnit.MEGABYTES
            )
        }
    }

    fun onNewBucketIdChange(id: String) {
        _uiState.update { it.copy(newBucketId = id) }
    }

    fun onNewBucketPublicToggle(isPublic: Boolean) {
        _uiState.update { it.copy(isNewBucketPublic = isPublic) }
    }

    fun onNewBucketSizeLimitToggle(enabled: Boolean) {
        _uiState.update { 
            it.copy(
                isNewBucketSizeLimitEnabled = enabled,
                newBucketFileSizeLimit = if (enabled && it.newBucketFileSizeLimit.isEmpty()) 
                    currentSettings?.fileSizeLimit?.toString() ?: "" 
                else it.newBucketFileSizeLimit,
                newBucketFileSizeUnit = if (enabled) 
                    currentSettings?.fileSizeUnit ?: SizeUnit.MEGABYTES 
                else it.newBucketFileSizeUnit
            )
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
        val fileSizeLimit = if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeLimit.toLongOrNull() else null
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
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                isUpdateBucketMode = false,
                                newBucketId = "",
                                isNewBucketPublic = true,
                                isNewBucketSizeLimitEnabled = false,
                                successMessage = "Bucket updated"
                            )
                        }
                        refreshManager.triggerRefreshBuckets()
                    },
                    onFailure = { error ->
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
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                newBucketId = "",
                                isNewBucketPublic = true,
                                isNewBucketSizeLimitEnabled = false,
                                successMessage = "Bucket created"
                            )
                        }
                        refreshManager.triggerRefreshBuckets()
                    },
                    onFailure = { error ->
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
                    _uiState.update { it.copy(isNewFolderDialogVisible = false, newFolderName = "", successMessage = "Folder created") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isNewFolderDialogVisible = false, newFolderName = "", error = error.message) }
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

            val fileName = platformFile.name
            val data = platformFile.readBytes()
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
        _uiState.update { it.copy(showCredentialSwitchConfirmation = false, selectedCredentialForSwitch = null) }
    }

    fun onConfirmCredentialSwitch() {
        val credential = _uiState.value.selectedCredentialForSwitch ?: return
        _uiState.update { it.copy(showCredentialSwitchConfirmation = false, isSettingUpCredential = true) }
        viewModelScope.launch {
            try {
                credentialRepository.setLastUsedId(credential.id)
                val newClient = supabaseClientManager.createClient(credential)
                // Optionally verify client
                supabaseClientManager.setClient(newClient)
                _navigateToBuckets.emit(Unit)
                _uiState.update { it.copy(lastUsedId = credential.id, isSettingUpCredential = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Switch failed: ${e.message}", isSettingUpCredential = false) }
            }
        }
    }

    fun onRemoveCredential(id: String) {
        viewModelScope.launch {
            credentialRepository.removeCredential(id)
            if (id == _uiState.value.lastUsedId) {
                // If removing current one, clear client
                supabaseClientManager.clearClient()
            }
        }
    }

    fun toggleViewMode() {
        val newViewMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(settings.copy(viewMode = newViewMode))
        }
    }

    fun onUpgradeClick() {
        purchaseRepository.triggerPaywall()
    }

    fun onRestoreClick() {
        _uiState.update { it.copy(isRestoring = true) }
        viewModelScope.launch {
            val result = purchaseRepository.restorePurchases()
            when (result) {
                is ApiResponse.Success -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            successMessage = "Restored successfully!"
                        )
                    }
                }
                is ApiResponse.Error -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            error = "Restore failed: ${result.exception.message}"
                        )
                    }
                }
                is ApiResponse.Loading -> { /* Handled by isRestoring = true */ }
            }
        }
    }
}
