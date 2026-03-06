package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.domain.usecase.ClearCacheUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.CreateBucketUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.CreateFolderUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteBucketUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.DownloadFileUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.EmptyBucketUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetFileMetadataUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.MoveFileUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.RefreshBucketContentsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.RefreshBucketsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.UploadFileUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::GetBucketsUseCase)
    singleOf(::GetBucketContentsUseCase)
    singleOf(::GetPublicUrlUseCase)
    singleOf(::DownloadFileUseCase)
    singleOf(::DeleteFileUseCase)
    singleOf(::GetFileMetadataUseCase)
    singleOf(::MoveFileUseCase)
    singleOf(::CreateFolderUseCase)
    singleOf(::UploadFileUseCase)
    singleOf(::EmptyBucketUseCase)
    singleOf(::CreateBucketUseCase)
    singleOf(::DeleteBucketUseCase)
    singleOf(::ClearCacheUseCase)
    singleOf(::RefreshBucketsUseCase)
    singleOf(::RefreshBucketContentsUseCase)
}
