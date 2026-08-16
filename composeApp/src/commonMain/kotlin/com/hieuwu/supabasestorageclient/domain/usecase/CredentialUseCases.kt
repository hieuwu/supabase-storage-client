package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.Credential
import kotlinx.coroutines.flow.Flow

interface ObserveCredentialsUseCase {
    operator fun invoke(): Flow<List<Credential>>
}

interface GetLastUsedCredentialIdUseCase {
    operator fun invoke(): String?
}

interface SetLastUsedCredentialIdUseCase {
    operator fun invoke(id: String)
}

interface AddCredentialUseCase {
    suspend operator fun invoke(name: String, url: String, key: String): Result<Unit>
}

interface UpdateCredentialUseCase {
    suspend operator fun invoke(id: String, name: String, url: String, key: String): Result<Unit>
}

interface DeleteCredentialUseCase {
    suspend operator fun invoke(id: String): Result<Unit>
}
