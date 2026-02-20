package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.util.getDirectoryPicker
import com.hieuwu.supabasestorageclient.util.getFileOpener
import com.hieuwu.supabasestorageclient.util.getFileWriter
import com.hieuwu.supabasestorageclient.util.getPermissionManager
import com.hieuwu.supabasestorageclient.data.repository.CredentialRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.OnboardingRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.StorageRepositoryImpl
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DownloadFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetFileMetadataUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.MoveFileUseCase
import com.hieuwu.supabasestorageclient.presentation.filebrowser.FileBrowserViewModel
import com.hieuwu.supabasestorageclient.presentation.buckets.BucketsViewModel
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsViewModel
import com.hieuwu.supabasestorageclient.presentation.fileview.FileViewViewModel
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import com.hieuwu.supabasestorageclient.presentation.downloads.DownloadsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    single { SupabaseClientManager() }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
    single<CredentialRepository> { CredentialRepositoryImpl(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get()) }
    single { getFileWriter() }
    single { getDirectoryPicker() }
    single { getFileOpener() }
    single { getPermissionManager() }
    single { DownloadManager(get(), get(), get()) }
}

val featureModule = module {
    viewModel { OnboardingViewModel(get()) }
    viewModel { CredentialsViewModel(get(), get()) }

    // Storage UseCases
    singleOf(::GetBucketsUseCase)
    singleOf(::GetBucketContentsUseCase)
    singleOf(::GetPublicUrlUseCase)
    singleOf(::DownloadFileUseCase)
    singleOf(::DeleteFileUseCase)
    singleOf(::GetFileMetadataUseCase)
    singleOf(::MoveFileUseCase)

    // Storage ViewModels
    viewModelOf(::BucketsViewModel)
    viewModel { (bucketId: String, path: String?) -> 
        FileBrowserViewModel(bucketId, path, get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { (bucketId: String, fileName: String, path: String?) ->
        FileViewViewModel(
            bucketId = bucketId,
            fileName = fileName,
            path = path,
            getPublicUrlUseCase = get(),
            deleteFileUseCase = get(),
            getFileMetadataUseCase = get(),
            clipboardManager = get(),
            downloadManager = get(),
            directoryPicker = get()
        )
    }
    viewModelOf(::DownloadsViewModel)
}

