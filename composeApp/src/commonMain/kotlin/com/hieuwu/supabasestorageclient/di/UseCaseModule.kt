package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.domain.usecase.impl.*
import com.hieuwu.supabasestorageclient.domain.usecase.*
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
    factory { GetStarredItemsUseCaseImpl(get()) } bind GetStarredItemsUseCase::class
    factory { ToggleStarUseCaseImpl(get()) } bind ToggleStarUseCase::class
    factory { GetUserSettingsUseCaseImpl(get()) } bind GetUserSettingsUseCase::class
    factory { UpdateUserSettingsUseCaseImpl(get()) } bind UpdateUserSettingsUseCase::class
    factory { UnstarItemUseCaseImpl(get()) } bind UnstarItemUseCase::class
    factory { ClearAllStarredItemsUseCaseImpl(get()) } bind ClearAllStarredItemsUseCase::class
    factory { ObserveUploadsUseCaseImpl(get()) } bind ObserveUploadsUseCase::class
    factory { CancelUploadUseCaseImpl(get()) } bind CancelUploadUseCase::class
    factory { ObserveProStatusUseCaseImpl(get()) } bind ObserveProStatusUseCase::class
    factory { RestorePurchasesUseCaseImpl(get()) } bind RestorePurchasesUseCase::class
    factory { DownloadFileUseCaseImpl(get()) } bind DownloadFileUseCase::class
    factory { IsOnboardingCompletedUseCaseImpl(get()) } bind IsOnboardingCompletedUseCase::class
    factory { MarkOnboardingCompletedUseCaseImpl(get()) } bind MarkOnboardingCompletedUseCase::class
    factory { ObserveCredentialsUseCaseImpl(get()) } bind ObserveCredentialsUseCase::class
    factory { GetLastUsedCredentialIdUseCaseImpl(get()) } bind GetLastUsedCredentialIdUseCase::class
    factory { SetLastUsedCredentialIdUseCaseImpl(get()) } bind SetLastUsedCredentialIdUseCase::class
    factory { AddCredentialUseCaseImpl(get(), get()) } bind AddCredentialUseCase::class
    factory { UpdateCredentialUseCaseImpl(get()) } bind UpdateCredentialUseCase::class
    factory { DeleteCredentialUseCaseImpl(get()) } bind DeleteCredentialUseCase::class
    factory { TriggerPaywallUseCaseImpl(get()) } bind TriggerPaywallUseCase::class
    factory { SwitchCredentialUseCaseImpl(get(), get(), get()) } bind SwitchCredentialUseCase::class
    factory { ObserveDownloadsUseCaseImpl(get()) } bind ObserveDownloadsUseCase::class
    factory { CancelDownloadUseCaseImpl(get()) } bind CancelDownloadUseCase::class
    factory { DeleteDownloadUseCaseImpl(get()) } bind DeleteDownloadUseCase::class
}
