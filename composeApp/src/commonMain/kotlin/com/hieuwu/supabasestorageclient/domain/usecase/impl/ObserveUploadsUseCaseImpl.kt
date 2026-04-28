package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import com.hieuwu.supabasestorageclient.domain.usecase.ObserveUploadsUseCase
import kotlinx.coroutines.flow.Flow

class ObserveUploadsUseCaseImpl(
    private val uploadManager: UploadManager
) : ObserveUploadsUseCase {
    override fun invoke(): Flow<List<UploadItem>> = uploadManager.uploads
}
