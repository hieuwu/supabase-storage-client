package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ClearCacheUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.SwitchCredentialUseCase
import io.github.jan.supabase.storage.storage

class SwitchCredentialUseCaseImpl(
    private val credentialRepository: CredentialRepository,
    private val supabaseClientManager: SupabaseClientManager,
    private val clearCacheUseCase: ClearCacheUseCase
) : SwitchCredentialUseCase {
    override suspend fun invoke(credential: Credential) {
        val currentId = credentialRepository.getLastUsedId()
        if (currentId != null) {
            clearCacheUseCase(ClearCacheUseCase.Params(currentId))
        }

        credentialRepository.setLastUsedId(credential.id)
        clearCacheUseCase(ClearCacheUseCase.Params(credential.id))
        
        val newClient = supabaseClientManager.createClient(credential)
        // Verify connection
        newClient.storage.listBuckets()
        
        supabaseClientManager.setClient(newClient)
    }
}
