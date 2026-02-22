package com.hieuwu.supabasestorageclient.presentation.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.github.jan.supabase.storage.storage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class CredentialsUiState(
    val credentials: List<Credential> = emptyList(),
    val lastUsedId: String? = null,
    val showCredentialsId: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSettingUp: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val credentialToDelete: Credential? = null,
    val showEditSheet: Boolean = false,
    val credentialToEdit: Credential? = null
)

class CredentialsViewModel(
    private val credentialRepository: CredentialRepository,
    private val supabaseClientManager: SupabaseClientManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            credentialRepository.getCredentials()
                .collect { credentials ->
                    val lastUsedId = credentialRepository.getLastUsedId()
                    _uiState.update { 
                        it.copy(
                            credentials = credentials,
                            lastUsedId = lastUsedId,
                            isLoading = false
                        )
                    }
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
            _uiState.update { it.copy(isSettingUp = true, error = null) }
            try {
                credentialRepository.setLastUsedId(credential.id)
                val newClient = supabaseClientManager.createClient(credential)

                newClient.storage.retrieveBuckets()
                
                supabaseClientManager.setClient(newClient)
                _uiState.update { it.copy(lastUsedId = credential.id) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Verification failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSettingUp = false) }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addCredential(name: String, url: String, key: String) {
        viewModelScope.launch {
            try {
                val newCredential = Credential(
                    id = Uuid.random().toString(),
                    name = name,
                    url = url,
                    key = key
                )
                credentialRepository.saveCredential(newCredential)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add credential: ${e.message}") }
            }
        }
    }

    fun updateCredential(id: String, name: String, url: String, key: String) {
        viewModelScope.launch {
            try {
                val updatedCredential = Credential(
                    id = id,
                    name = name,
                    url = url,
                    key = key
                )
                credentialRepository.saveCredential(updatedCredential)
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
                credentialRepository.removeCredential(id)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete credential: ${e.message}") }
            }
        }
    }
}
