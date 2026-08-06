package com.hieuwu.supabasestorageclient.domain.usecase

interface CancelUploadUseCase {
    operator fun invoke(id: String)
}
