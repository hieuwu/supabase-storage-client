package com.hieuwu.supabasestorageclient.domain.usecase

import kotlinx.coroutines.flow.Flow

interface ObserveProStatusUseCase {
    operator fun invoke(): Flow<Boolean>
}
