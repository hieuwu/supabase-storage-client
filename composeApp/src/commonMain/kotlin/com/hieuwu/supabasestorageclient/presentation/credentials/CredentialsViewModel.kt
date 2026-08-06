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
            observeProStatusUseCase().collect { isPro ->
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
            try {
                switchCredentialUseCase(credential)
                _uiState.update { it.copy(lastUsedId = credential.id) }
            } catch (e: Exception) {
                logger.e(e) { "Credential selection failed" }
                val errorMessage = e.message ?: e.toString()
                _uiState.update { it.copy(error = "Verification failed: $errorMessage") }
            } finally {
                _uiState.update { it.copy(isSettingUp = false) }
            }
        }
    }

    fun addCredential(name: String, url: String, key: String) {
        viewModelScope.launch {
            try {
                val isPro = _uiState.value.isPro
                val currentCount = _uiState.value.credentials.size
                if (!isPro && currentCount >= 2) {
                    triggerPaywallUseCase()
                    return@launch
                }
                addCredentialUseCase(name, url, key)
                hideAddSheet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add credential: ${e.message}") }
            }
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
            try {
                updateCredentialUseCase(id, name, url, key)
                hideEditSheet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update credential: ${e.message}") }
            }
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
            try {
                deleteCredentialUseCase(id)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete credential: ${e.message}") }
            }
        }
    }
}
