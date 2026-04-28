package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import kotlinx.coroutines.flow.Flow

interface ObserveUploadsUseCase {
    operator fun invoke(): Flow<List<UploadItem>>
}
