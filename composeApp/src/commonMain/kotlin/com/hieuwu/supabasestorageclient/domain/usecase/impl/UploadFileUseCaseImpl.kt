package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import com.hieuwu.supabasestorageclient.domain.usecase.UploadFileUseCase

class UploadFileUseCaseImpl(
    private val uploadManager: UploadManager
) : UploadFileUseCase {
    override fun invoke(params: UploadFileUseCase.Params) {
        uploadManager.upload(params.bucketId, params.path, params.fileName, params.data)
    }
}
