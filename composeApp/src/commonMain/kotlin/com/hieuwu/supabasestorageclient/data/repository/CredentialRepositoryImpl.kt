package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CredentialRepositoryImpl(
    private val settings: Settings
) : CredentialRepository {

    private val _credentials = MutableStateFlow<List<Credential>>(emptyList())
    
    companion object {
        private const val KEY_CREDENTIALS = "supabase_credentials"
        private const val KEY_LAST_USED_ID = "last_used_credential_id"
    }

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        val json = settings.getString(KEY_CREDENTIALS, "[]")
        try {
            val list = Json.decodeFromString<List<Credential>>(json)
            _credentials.value = list
        } catch (e: Exception) {
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
        val json = Json.encodeToString(_credentials.value)
        settings[KEY_CREDENTIALS] = json
    }

    override fun getLastUsedId(): String? {
        return settings.getStringOrNull(KEY_LAST_USED_ID)
    }

    override fun setLastUsedId(id: String) {
        settings[KEY_LAST_USED_ID] = id
    }
}
