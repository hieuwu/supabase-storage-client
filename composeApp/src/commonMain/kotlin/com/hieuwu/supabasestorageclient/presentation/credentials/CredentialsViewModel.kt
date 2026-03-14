package com.hieuwu.supabasestorageclient.presentation.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.github.jan.supabase.storage.storage
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ClearCacheUseCase
import kotlinx.coroutines.delay
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CredentialsViewModel(
    private val credentialRepository: CredentialRepository,
    private val supabaseClientManager: SupabaseClientManager,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val purchaseRepository: PurchaseRepository,
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

            purchaseRepository.isPro.collect { isPro ->
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
                // Clear cache for the current credential before switching
                val currentId = credentialRepository.getLastUsedId()
                if (currentId != null) {
                    clearCacheUseCase(ClearCacheUseCase.Params(currentId))
                }

                credentialRepository.setLastUsedId(credential.id)
                
                // Clear cache for the new credential to ensure a fresh state
                clearCacheUseCase(ClearCacheUseCase.Params(credential.id))
                
                val newClient = supabaseClientManager.createClient(credential)
                delay(5000)

                logger.d { "Verifying new client connection..." }
                newClient.storage.retrieveBuckets()
                
                supabaseClientManager.setClient(newClient)
                logger.d { "Active credential switched to ${credential.name}" }
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

    @OptIn(ExperimentalUuidApi::class)
    fun addCredential(name: String, url: String, key: String) {
        viewModelScope.launch {
            try {
                val isPro = purchaseRepository.isPro.value
                val currentCount = _uiState.value.credentials.size
                if (!isPro && currentCount >= 2) {
                    purchaseRepository.triggerPaywall()
                    return@launch
                }

                val sanitizedUrl = url.trim().split(Regex("\\s+")).firstOrNull() ?: ""
                val sanitizedKey = key.trim().split(Regex("\\s+")).firstOrNull() ?: ""
                val newCredential = Credential(
                    id = Uuid.random().toString(),
                    name = name.trim(),
                    url = sanitizedUrl,
                    key = sanitizedKey
                )
                credentialRepository.saveCredential(newCredential)
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
            purchaseRepository.triggerPaywall()
        }
    }

    fun hideAddSheet() {
        _uiState.update { it.copy(showAddSheet = false) }
    }

    fun updateCredential(id: String, name: String, url: String, key: String) {
        viewModelScope.launch {
            try {
                val sanitizedUrl = url.trim().split(Regex("\\s+")).firstOrNull() ?: ""
                val sanitizedKey = key.trim().split(Regex("\\s+")).firstOrNull() ?: ""
                val updatedCredential = Credential(
                    id = id,
                    name = name.trim(),
                    url = sanitizedUrl,
                    key = sanitizedKey
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
