package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import com.hieuwu.supabasestorageclient.domain.usecase.CancelUploadUseCase

class CancelUploadUseCaseImpl(
    private val uploadManager: UploadManager
) : CancelUploadUseCase {
    override fun invoke(id: String) {
        uploadManager.cancel(id)
    }
}
