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
import com.hieuwu.supabasestorageclient.util.FilePicker
import com.hieuwu.supabasestorageclient.util.PermissionManager
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    private val contextSelectionManager: ContextSelectionManager,
    private val createFolderUseCase: CreateFolderUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val createBucketUseCase: CreateBucketUseCase,
    private val filePicker: FilePicker,
    private val permissionManager: PermissionManager,
    private val credentialRepository: CredentialRepository,
    private val settingsRepository: SettingsRepository,
    private val supabaseClientManager: SupabaseClientManager
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
                isNewBucketSizeLimitEnabled = false,
                newBucketFileSizeLimit = currentSettings?.fileSizeLimit?.toString() ?: "",
                newBucketFileSizeUnit = currentSettings?.fileSizeUnit ?: SizeUnit.MEGABYTES
            )
        }
    }

    fun onDismissCreateBucketDialog() {
        _uiState.update {
            it.copy(
                isCreateBucketDialogVisible = false,
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
                            newBucketFileSizeLimit = currentSettings?.fileSizeLimit?.toString() ?: "",
                            newBucketFileSizeUnit = currentSettings?.fileSizeUnit ?: SizeUnit.MEGABYTES,
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

    fun onUploadFileClick() {
        viewModelScope.launch {
            if (!permissionManager.requestStoragePermission()) {
                _uiState.update { it.copy(error = "Permission denied") }
                return@launch
            }

            val context = contextSelectionManager.currentContext.value
            if (context == null) {
                _uiState.update { it.copy(error = "Please select a bucket first") }
                return@launch
            }

            val selectedFile = filePicker.pickFile()
            if (selectedFile != null) {
                val fullPath = if (context.path.isEmpty()) selectedFile.name else "${context.path}/${selectedFile.name}"
                val params = UploadFileUseCase.Params(
                    bucketId = context.bucketId,
                    path = fullPath,
                    fileName = selectedFile.name,
                    data = selectedFile.data
                )
                uploadFileUseCase(params)
                _uiState.update { it.copy(successMessage = "Upload started", isUploading = true) }
                _navigateToUploads.emit(Unit)
            }
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
}
