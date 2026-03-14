package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.Credential
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.StateFlow

interface CredentialRepository {
    val lastUsedId: StateFlow<String?>
    fun getCredentials(): Flow<List<Credential>>
    suspend fun saveCredential(credential: Credential)
    suspend fun removeCredential(id: String)
    fun getLastUsedId(): String?
    fun setLastUsedId(id: String)
}
