package com.hieuwu.supabasestorageclient.presentation.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CredentialsViewModel(
    private val observeCredentialsUseCase: ObserveCredentialsUseCase,
    private val getLastUsedCredentialIdUseCase: GetLastUsedCredentialIdUseCase,
    private val addCredentialUseCase: AddCredentialUseCase,
    private val updateCredentialUseCase: UpdateCredentialUseCase,
    private val deleteCredentialUseCase: DeleteCredentialUseCase,
    private val triggerPaywallUseCase: TriggerPaywallUseCase,
    private val switchCredentialUseCase: SwitchCredentialUseCase,
    private val observeProStatusUseCase: ObserveProStatusUseCase,
    private val supabaseClientManager: SupabaseClientManager,
    private val logger: co.touchlab.kermit.Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            observeCredentialsUseCase()
                .catch { error ->
                    logger.e(error) { "Failed to observe credentials" }
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load credentials: ${error.message}") }
                }
                .collect { credentials ->
                    val lastUsedId = getLastUsedCredentialIdUseCase()
                    _uiState.update {
                        it.copy(
                            credentials = credentials,
                            lastUsedId = lastUsedId,
                            isLoading = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            observeProStatusUseCase()
                .catch { error -> logger.e(error) { "Failed to observe pro status" } }
                .collect { isPro ->
                    _uiState.update { it.copy(isPro = isPro) }
                }
        }
    }

    fun toggleCredentialVisibility(id: String) {
        _uiState.update { state ->
            val newSet = if (state.showCredentialsId.contains(id)) {
                state.showCredentialsId - id
            } else {
                state.showCredentialsId + id
            }
            state.copy(showCredentialsId = newSet)
        }
    }

    fun selectCredential(credential: Credential) {
        viewModelScope.launch {
            logger.d { "Selecting credential: ${credential.name} (${credential.id})" }
            _uiState.update { it.copy(isSettingUp = true, error = null) }
            switchCredentialUseCase(credential).fold(
                onSuccess = {
                    _uiState.update { it.copy(lastUsedId = credential.id, isSettingUp = false) }
                },
                onFailure = { error ->
                    logger.e(error) { "Credential selection failed for ${credential.id}" }
                    val errorMessage = error.message ?: error.toString()
                    _uiState.update {
                        it.copy(error = "Verification failed: $errorMessage", isSettingUp = false)
                    }
                }
            )
        }
    }

    fun addCredential(name: String, url: String, key: String) {
        viewModelScope.launch {
            val isPro = _uiState.value.isPro
            val currentCount = _uiState.value.credentials.size
            if (!isPro && currentCount >= 2) {
                triggerPaywallUseCase()
                return@launch
            }
            addCredentialUseCase(name, url, key).fold(
                onSuccess = { hideAddSheet() },
                onFailure = { error ->
                    logger.e(error) { "Failed to add credential '$name'" }
                    _uiState.update { it.copy(error = "Failed to add credential: ${error.message}") }
                }
            )
        }
    }

    fun onAddClick() {
        val uiState = _uiState.value
        if (uiState.isPro || uiState.credentials.size < 2) {
            _uiState.update { it.copy(showAddSheet = true) }
        } else {
            triggerPaywallUseCase()
        }
    }

    fun hideAddSheet() {
        _uiState.update { it.copy(showAddSheet = false) }
    }

    fun updateCredential(id: String, name: String, url: String, key: String) {
        viewModelScope.launch {
            updateCredentialUseCase(id, name, url, key).fold(
                onSuccess = { hideEditSheet() },
                onFailure = { error ->
                    logger.e(error) { "Failed to update credential $id" }
                    _uiState.update { it.copy(error = "Failed to update credential: ${error.message}") }
                }
            )
        }
    }

    fun showEditSheet(credential: Credential) {
        _uiState.update { it.copy(showEditSheet = true, credentialToEdit = credential) }
    }

    fun hideEditSheet() {
        _uiState.update { it.copy(showEditSheet = false, credentialToEdit = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showDeleteDialog(credential: Credential) {
        _uiState.update { it.copy(showDeleteConfirmation = true, credentialToDelete = credential) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false, credentialToDelete = null) }
    }

    fun deleteCredential(id: String) {
        viewModelScope.launch {
            deleteCredentialUseCase(id).fold(
                onSuccess = { hideDeleteDialog() },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete credential $id" }
                    _uiState.update { it.copy(error = "Failed to delete credential: ${error.message}") }
                }
            )
        }
    }
}
