package com.hieuwu.supabasestorageclient.presentation.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class CredentialsUiState(
    val credentials: List<Credential> = emptyList(),
    val lastUsedId: String? = null,
    val showCredentialsId: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
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
                    
                    // Auto-select last used if available and client not initialized
                    if (supabaseClientManager.client.value == null && lastUsedId != null) {
                        credentials.find { it.id == lastUsedId }?.let {
                            supabaseClientManager.selectCredential(it)
                        }
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
            credentialRepository.setLastUsedId(credential.id)
            supabaseClientManager.selectCredential(credential)
            _uiState.update { it.copy(lastUsedId = credential.id) }
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
                selectCredential(newCredential)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add credential: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
