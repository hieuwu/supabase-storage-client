package com.hieuwu.supabasestorageclient

import com.hieuwu.supabasestorageclient.domain.model.Credential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseClientManager {

    private val _client = MutableStateFlow<SupabaseClient?>(null)
    val client: StateFlow<SupabaseClient?> = _client.asStateFlow()

    fun createClient(credential: Credential): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = credential.url,
            supabaseKey = credential.key
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
