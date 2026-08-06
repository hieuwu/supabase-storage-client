package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.data.dto.CredentialDto
import com.hieuwu.supabasestorageclient.data.dto.toDomain
import com.hieuwu.supabasestorageclient.data.dto.toDto
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CredentialRepositoryImpl(
    private val settings: Settings,
    private val logger: Logger
) : CredentialRepository {

    private val _credentials = MutableStateFlow<List<Credential>>(emptyList())
    private val _lastUsedId = MutableStateFlow<String?>(null)
    override val lastUsedId: StateFlow<String?> = _lastUsedId.asStateFlow()
    
    companion object {
        private const val KEY_CREDENTIALS = "supabase_credentials"
        private const val KEY_LAST_USED_ID = "last_used_credential_id"
    }

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        val json = settings.getString(KEY_CREDENTIALS, "[]")
        _lastUsedId.value = settings.getStringOrNull(KEY_LAST_USED_ID)
        try {
            val list = Json.decodeFromString<List<CredentialDto>>(json)
            _credentials.value = list.map { it.toDomain() }
        } catch (e: Exception) {
            logger.e(e) { "Error decoding credentials from settings" }
            _credentials.value = emptyList()
        }
    }

    override fun getCredentials(): Flow<List<Credential>> {
        return _credentials.asStateFlow()
    }

    override suspend fun saveCredential(credential: Credential) {
        val currentList = _credentials.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == credential.id }
        if (index != -1) {
            currentList[index] = credential
        } else {
            currentList.add(credential)
        }
        _credentials.value = currentList
        persistCredentials()
    }

    override suspend fun removeCredential(id: String) {
        val currentList = _credentials.value.toMutableList()
        currentList.removeAll { it.id == id }
        _credentials.value = currentList
        persistCredentials()
    }

    private fun persistCredentials() {
        val dtos = _credentials.value.map { it.toDto() }
        val json = Json.encodeToString(dtos)
        settings[KEY_CREDENTIALS] = json
    }

    override fun getLastUsedId(): String? {
        return settings.getStringOrNull(KEY_LAST_USED_ID)
    }

    override fun setLastUsedId(id: String) {
        settings[KEY_LAST_USED_ID] = id
        _lastUsedId.value = id
    }
}
