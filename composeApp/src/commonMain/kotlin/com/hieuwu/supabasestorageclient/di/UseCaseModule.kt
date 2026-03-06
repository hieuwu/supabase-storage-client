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
import com.hieuwu.supabasestorageclient.domain.usecase.impl.ClearCacheUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.CreateBucketUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.CreateFolderUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.DeleteBucketUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.DeleteFileUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.DownloadFileUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.EmptyBucketUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.GetBucketContentsUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.GetBucketsUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.GetFileMetadataUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.GetPublicUrlUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.MoveFileUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.RefreshBucketContentsUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.RefreshBucketsUseCaseImpl
import com.hieuwu.supabasestorageclient.domain.usecase.impl.UploadFileUseCaseImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetBucketsUseCaseImpl(get()) } bind GetBucketsUseCase::class
    factory { GetBucketContentsUseCaseImpl(get()) } bind GetBucketContentsUseCase::class
    factory { GetPublicUrlUseCaseImpl(get()) } bind GetPublicUrlUseCase::class
    factory { DownloadFileUseCaseImpl(get()) } bind DownloadFileUseCase::class
    factory { DeleteFileUseCaseImpl(get()) } bind DeleteFileUseCase::class
    factory { GetFileMetadataUseCaseImpl(get()) } bind GetFileMetadataUseCase::class
    factory { MoveFileUseCaseImpl(get()) } bind MoveFileUseCase::class
    factory { CreateFolderUseCaseImpl(get()) } bind CreateFolderUseCase::class
    factory { UploadFileUseCaseImpl(get()) } bind UploadFileUseCase::class
    factory { EmptyBucketUseCaseImpl(get()) } bind EmptyBucketUseCase::class
    factory { CreateBucketUseCaseImpl(get()) } bind CreateBucketUseCase::class
    factory { DeleteBucketUseCaseImpl(get()) } bind DeleteBucketUseCase::class
    factory { ClearCacheUseCaseImpl(get()) } bind ClearCacheUseCase::class
    factory { RefreshBucketsUseCaseImpl(get()) } bind RefreshBucketsUseCase::class
    factory { RefreshBucketContentsUseCaseImpl(get()) } bind RefreshBucketContentsUseCase::class
}
