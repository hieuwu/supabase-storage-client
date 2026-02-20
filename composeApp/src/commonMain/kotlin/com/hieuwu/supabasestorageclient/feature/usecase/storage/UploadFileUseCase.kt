package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.upload.UploadManager

class UploadFileUseCase(private val uploadManager: UploadManager) {
    operator fun invoke(bucketId: String, path: String, fileName: String, data: ByteArray) {
        uploadManager.upload(bucketId, path, fileName, data)
    }
}
