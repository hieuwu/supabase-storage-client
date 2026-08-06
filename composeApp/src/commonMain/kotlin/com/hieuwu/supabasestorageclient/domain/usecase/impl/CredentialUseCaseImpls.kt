package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.usecase.*
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ObserveCredentialsUseCaseImpl(
    private val repository: CredentialRepository
) : ObserveCredentialsUseCase {
    override fun invoke(): Flow<List<Credential>> = repository.getCredentials()
}

class GetLastUsedCredentialIdUseCaseImpl(
    private val repository: CredentialRepository
) : GetLastUsedCredentialIdUseCase {
    override fun invoke(): String? = repository.getLastUsedId()
}

class SetLastUsedCredentialIdUseCaseImpl(
    private val repository: CredentialRepository
) : SetLastUsedCredentialIdUseCase {
    override fun invoke(id: String) = repository.setLastUsedId(id)
}

class AddCredentialUseCaseImpl(
    private val credentialRepository: CredentialRepository,
    private val purchaseRepository: PurchaseRepository
) : AddCredentialUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(name: String, url: String, key: String) {
        val sanitizedUrl = url.trim().split(Regex("\\s+")).firstOrNull() ?: ""
        val sanitizedKey = key.trim().split(Regex("\\s+")).firstOrNull() ?: ""
        val newCredential = Credential(
            id = Uuid.random().toString(),
            name = name.trim(),
            url = sanitizedUrl,
            key = sanitizedKey
        )
        credentialRepository.saveCredential(newCredential)
    }
}

class UpdateCredentialUseCaseImpl(
    private val credentialRepository: CredentialRepository
) : UpdateCredentialUseCase {
    override suspend fun invoke(id: String, name: String, url: String, key: String) {
        val sanitizedUrl = url.trim().split(Regex("\\s+")).firstOrNull() ?: ""
        val sanitizedKey = key.trim().split(Regex("\\s+")).firstOrNull() ?: ""
        val updatedCredential = Credential(
            id = id,
            name = name.trim(),
            url = sanitizedUrl,
            key = sanitizedKey
        )
        credentialRepository.saveCredential(updatedCredential)
    }
}

class DeleteCredentialUseCaseImpl(
    private val credentialRepository: CredentialRepository
) : DeleteCredentialUseCase {
    override suspend fun invoke(id: String) {
        credentialRepository.removeCredential(id)
    }
}
