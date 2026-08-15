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
        _credentials.value = runCatching {
            val json = settings.getString(KEY_CREDENTIALS, "[]")
            Json.decodeFromString<List<CredentialDto>>(json).map { it.toDomain() }
        }.getOrElse { error ->
            logger.e(error) { "Error decoding credentials from settings, starting with an empty list" }
            emptyList()
        }
        _lastUsedId.value = runCatching { settings.getStringOrNull(KEY_LAST_USED_ID) }
            .getOrElse { error ->
                logger.e(error) { "Error reading last used credential id from settings" }
                null
            }
    }

    override fun getCredentials(): Flow<List<Credential>> {
        return _credentials.asStateFlow()
    }

    override suspend fun saveCredential(credential: Credential): Result<Unit> {
        val previous = _credentials.value
        val currentList = previous.toMutableList()
        val index = currentList.indexOfFirst { it.id == credential.id }
        if (index != -1) {
            currentList[index] = credential
        } else {
            currentList.add(credential)
        }
        return persist(currentList, previous, "save credential ${credential.id}")
    }

    override suspend fun removeCredential(id: String): Result<Unit> {
        val previous = _credentials.value
        return persist(previous.filterNot { it.id == id }, previous, "remove credential $id")
    }

    /**
     * Publishes [next], then writes it out. If the write fails the in-memory list is rolled back to
     * [previous] so what the UI shows always matches what is on disk.
     */
    private fun persist(
        next: List<Credential>,
        previous: List<Credential>,
        description: String
    ): Result<Unit> {
        _credentials.value = next
        return runCatching {
            settings[KEY_CREDENTIALS] = Json.encodeToString(next.map { it.toDto() })
        }.onFailure { error ->
            logger.e(error) { "Failed to $description, reverting in-memory credentials" }
            _credentials.value = previous
        }
    }

    override fun getLastUsedId(): String? {
        // Called from non-suspend hot paths (download/upload bookkeeping), so a settings failure
        // must degrade to "no credential" rather than propagate.
        return runCatching { settings.getStringOrNull(KEY_LAST_USED_ID) }
            .getOrElse { error ->
                logger.e(error) { "Failed to read last used credential id" }
                null
            }
    }

    override fun setLastUsedId(id: String): Result<Unit> =
        runCatching { settings[KEY_LAST_USED_ID] = id }
            .onSuccess { _lastUsedId.value = id }
            .onFailure { error -> logger.e(error) { "Failed to persist last used credential id $id" } }
}
