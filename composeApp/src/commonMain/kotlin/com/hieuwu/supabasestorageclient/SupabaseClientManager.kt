package com.hieuwu.supabasestorageclient

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class SupabaseCredential(
    val id: String, // unique identifier
    val name: String,
    val url: String,
    val key: String
)

class SupabaseClientManager(private val settings: Settings) {

    private val _client = MutableStateFlow<SupabaseClient?>(null)
    val client: StateFlow<SupabaseClient?> = _client.asStateFlow()

    private val _credentials = MutableStateFlow<List<SupabaseCredential>>(emptyList())
    val credentials: StateFlow<List<SupabaseCredential>> = _credentials.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        val json = settings.getString("supabase_credentials", "[]")
        try {
            val list = Json.decodeFromString<List<SupabaseCredential>>(json)
            _credentials.value = list
            // Optionally auto-select the last used one?
            val lastUsedId = settings.getStringOrNull("last_used_credential_id")
            if (lastUsedId != null) {
                list.find { it.id == lastUsedId }?.let { selectCredential(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _credentials.value = emptyList()
        }
    }

    fun saveCredential(credential: SupabaseCredential) {
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

    fun removeCredential(id: String) {
        val currentList = _credentials.value.toMutableList()
        currentList.removeIf { it.id == id }
        _credentials.value = currentList
        persistCredentials()
        
        // If current client is using this credential, maybe clear it?
        // logic here depending on requirement. For now, just remove from list.
    }

    private fun persistCredentials() {
        val json = Json.encodeToString(_credentials.value)
        settings["supabase_credentials"] = json
    }

    fun selectCredential(credential: SupabaseCredential) {
        try {
            val newClient = createSupabaseClient(
                supabaseUrl = credential.url,
                supabaseKey = credential.key
            ) {
                install(Postgrest)
                install(Storage)
                install(Auth)
                install(Realtime)
            }
            _client.value = newClient
            settings["last_used_credential_id"] = credential.id
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error (notify user? throw?)
        }
    }
}
