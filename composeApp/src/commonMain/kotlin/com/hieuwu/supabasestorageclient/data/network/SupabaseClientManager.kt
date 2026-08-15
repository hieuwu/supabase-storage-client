package com.hieuwu.supabasestorageclient.data.network

import com.hieuwu.supabasestorageclient.domain.model.Credential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseClientManager {

    private val _client = MutableStateFlow<SupabaseClient?>(null)
    val client: StateFlow<SupabaseClient?> = _client.asStateFlow()

    /**
     * Throws when the url or key is unusable so the caller can report it - callers wrap this in
     * `runCatching` rather than getting a half-configured client.
     */
    fun createClient(credential: Credential): SupabaseClient {
        val sanitizedUrl = credential.url.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val sanitizedKey = credential.key.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        require(sanitizedUrl.isNotEmpty()) { "Supabase URL is empty for credential '${credential.name}'" }
        require(sanitizedKey.isNotEmpty()) { "Supabase key is empty for credential '${credential.name}'" }
        return createSupabaseClient(
            supabaseUrl = sanitizedUrl,
            supabaseKey = sanitizedKey
        ) {
            install(Postgrest)
            install(Storage)
            install(Auth)
        }
    }

    fun setClient(client: SupabaseClient) {
        _client.value = client
    }

    fun clearClient() {
        _client.value = null
    }
}