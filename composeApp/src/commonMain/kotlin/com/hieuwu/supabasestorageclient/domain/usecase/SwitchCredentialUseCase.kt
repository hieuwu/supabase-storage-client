package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.Credential

interface SwitchCredentialUseCase {
    suspend operator fun invoke(credential: Credential)
}
