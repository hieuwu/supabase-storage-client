package com.hieuwu.supabasestorageclient.domain.usecase.impl

import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ClearCacheUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.SwitchCredentialUseCase
import io.github.jan.supabase.storage.storage

class SwitchCredentialUseCaseImpl(
    private val credentialRepository: CredentialRepository,
    private val supabaseClientManager: SupabaseClientManager,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val logger: Logger
) : SwitchCredentialUseCase {
    override suspend fun invoke(credential: Credential): Result<Unit> {
        val currentId = credentialRepository.getLastUsedId()
        if (currentId != null) {
            // A stale cache is not worth failing the switch over - just report it.
            clearCacheUseCase(ClearCacheUseCase.Params(currentId)).onFailure { error ->
                logger.w(error) { "Failed to clear cache for previous credential $currentId" }
            }
        }

        return credentialRepository.setLastUsedId(credential.id)
            .mapCatching {
                clearCacheUseCase(ClearCacheUseCase.Params(credential.id)).onFailure { error ->
                    logger.w(error) { "Failed to clear cache for credential ${credential.id}" }
                }

                val newClient = supabaseClientManager.createClient(credential)
                // Verify the connection before publishing the client, so a bad url or key is
                // reported here instead of failing every later screen.
                newClient.storage.listBuckets()

                supabaseClientManager.setClient(newClient)
            }
            .onFailure { error ->
                logger.e(error) { "Failed to switch to credential ${credential.name} (${credential.id})" }
            }
    }
}
