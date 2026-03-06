package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.upload.UploadManager

fun interface UploadFileUseCase {
    data class Params(
        val bucketId: String,
        val path: String,
        val fileName: String,
        val data: ByteArray
    )

    operator fun invoke(params: Params)
}
