package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.SizeUnit

fun interface UpdateBucketUseCase {
    data class Params(
        val id: String,
        val public: Boolean,
        val fileSizeLimit: Long?,
        val unit: SizeUnit?
    )

    suspend operator fun invoke(params: Params): Result<Unit>
}
