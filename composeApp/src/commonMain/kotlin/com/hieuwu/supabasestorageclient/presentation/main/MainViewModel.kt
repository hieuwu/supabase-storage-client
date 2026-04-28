package com.hieuwu.supabasestorageclient.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val contextSelectionManager: ContextSelectionManager,
    private val createFolderUseCase: CreateFolderUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val createBucketUseCase: CreateBucketUseCase,
    private val observeCredentialsUseCase: ObserveCredentialsUseCase,
    private val getLastUsedCredentialIdUseCase: GetLastUsedCredentialIdUseCase,
    private val observeUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val switchCredentialUseCase: SwitchCredentialUseCase,
    private val deleteCredentialUseCase: DeleteCredentialUseCase,
    private val triggerPaywallUseCase: TriggerPaywallUseCase,
    private val observeProStatusUseCase: ObserveProStatusUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase
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

    private fun observeData() {
        viewModelScope.launch {
            observeCredentialsUseCase().collect { credentials ->
                val lastUsedId = getLastUsedCredentialIdUseCase()
                _uiState.update { it.copy(credentials = credentials, lastUsedId = lastUsedId) }
            }
        }

        viewModelScope.launch {
            observeUserSettingsUseCase().collect { settings ->
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
            observeProStatusUseCase().collect { isPro ->
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
            val settings = observeUserSettingsUseCase().first()
            _uiState.update { 
                it.copy(
                    isCreateBucketDialogVisible = true,
                    isNewBucketSizeLimitEnabled = false,
                    newBucketFileSizeLimit = settings.fileSizeLimit.toString(),
                    newBucketFileSizeUnit = settings.fileSizeUnit
                )
            }
        }
    }

    fun onDismissCreateBucketDialog() {
        viewModelScope.launch {
            val settings = observeUserSettingsUseCase().first()
            _uiState.update {
                it.copy(
                    isCreateBucketDialogVisible = false,
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
            val settings = observeUserSettingsUseCase().first()
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
        val fileSizeLimit = if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeLimit.toLongOrNull() else null
        val isPublic = _uiState.value.isNewBucketPublic
        val unit = if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeUnit else null

        viewModelScope.launch {
            val params = CreateBucketUseCase.Params(
                id = id,
                public = isPublic,
                fileSizeLimit = fileSizeLimit,
                unit = unit
            )
            createBucketUseCase(params).fold(
                onSuccess = {
                    val settings = observeUserSettingsUseCase().first()
                    _uiState.update {
                        it.copy(
                            isCreateBucketDialogVisible = false,
                            newBucketId = "",
                            isNewBucketPublic = true,
                            isNewBucketSizeLimitEnabled = false,
                            newBucketFileSizeLimit = settings.fileSizeLimit.toString(),
                            newBucketFileSizeUnit = settings.fileSizeUnit,
                            successMessage = "Bucket created"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
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
                switchCredentialUseCase(credential)
                _navigateToBuckets.emit(Unit)
                _uiState.update { it.copy(lastUsedId = credential.id, isSettingUpCredential = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Switch failed: ${e.message}", isSettingUpCredential = false) }
            }
        }
    }

    fun onRemoveCredential(id: String) {
        viewModelScope.launch {
            deleteCredentialUseCase(id)
            // If current was removed, the UseCase or Manager should handle clearing technically,
            // but for UI we might need to react if not already doing so via credentials stream.
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val settings = observeUserSettingsUseCase().first()
            val newViewMode = if (settings.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            updateUserSettingsUseCase(settings.copy(viewMode = newViewMode))
        }
    }

    fun onUpgradeClick() {
        triggerPaywallUseCase()
    }

    fun onRestoreClick() {
        _uiState.update { it.copy(isRestoring = true) }
        viewModelScope.launch {
            val result = restorePurchasesUseCase()
            _uiState.update { it.copy(isRestoring = false) }
            when (result) {
                is ApiResponse.Success -> {
                    _uiState.update { it.copy(successMessage = "Restored successfully!") }
                }
                is ApiResponse.Error -> {
                    _uiState.update { it.copy(error = "Restore failed: ${result.exception.message}") }
                }
                is ApiResponse.Loading -> { }
            }
        }
    }
}
