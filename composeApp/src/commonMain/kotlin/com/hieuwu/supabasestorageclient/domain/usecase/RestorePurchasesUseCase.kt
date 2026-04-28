package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.data.network.ApiResponse

interface RestorePurchasesUseCase {
    suspend operator fun invoke(): ApiResponse<Unit>
}
