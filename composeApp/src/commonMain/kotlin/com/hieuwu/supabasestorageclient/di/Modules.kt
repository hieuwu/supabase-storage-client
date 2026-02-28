package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.util.getDirectoryPicker
import com.hieuwu.supabasestorageclient.util.getFilePicker
import com.hieuwu.supabasestorageclient.util.getFileOpener
import com.hieuwu.supabasestorageclient.util.getFileWriter
import com.hieuwu.supabasestorageclient.util.getPermissionManager
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.data.repository.CredentialRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.OnboardingRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.StorageRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.SettingsRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.DownloadRepositoryImpl
import com.hieuwu.supabasestorageclient.domain.repository.DownloadRepository
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import com.hieuwu.supabasestorageclient.data.repository.StarredRepositoryImpl
import com.hieuwu.supabasestorageclient.domain.repository.UploadRepository
import com.hieuwu.supabasestorageclient.data.repository.UploadRepositoryImpl
import com.hieuwu.supabasestorageclient.feature.usecase.storage.CreateBucketUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.CreateFolderUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DownloadFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetFileMetadataUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.MoveFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.UploadFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.EmptyBucketUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteBucketUseCase
import com.hieuwu.supabasestorageclient.presentation.filebrowser.FileBrowserViewModel
import com.hieuwu.supabasestorageclient.presentation.buckets.BucketsViewModel
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsViewModel
import com.hieuwu.supabasestorageclient.presentation.fileview.FileViewViewModel
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import com.hieuwu.supabasestorageclient.presentation.downloads.DownloadsViewModel
import com.hieuwu.supabasestorageclient.presentation.main.MainViewModel
import com.hieuwu.supabasestorageclient.presentation.uploads.UploadViewModel
import com.hieuwu.supabasestorageclient.presentation.search.SearchViewModel
import com.hieuwu.supabasestorageclient.presentation.settings.SettingsViewModel
import com.hieuwu.supabasestorageclient.presentation.starred.StarredViewModel
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.feature.usecase.storage.ClearCacheUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.RefreshBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.RefreshBucketsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    single { Logger.withTag("SupabaseStorageClient") }
    single { SupabaseClientManager() }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get(), get()) }
    single<CredentialRepository> { CredentialRepositoryImpl(get(), get()) }
    single { com.hieuwu.supabasestorageclient.database.AppDatabase(get<com.hieuwu.supabasestorageclient.database.DatabaseDriverFactory>().createDriver()) }
    single<StorageRepository> { StorageRepositoryImpl(get(), get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single { getFileWriter() }
    single { getDirectoryPicker() }
    single { getFilePicker() }
    single { getFileOpener() }
    single { getPermissionManager() }
    single<DownloadRepository> { DownloadRepositoryImpl(get()) }
    single<StarredRepository> { StarredRepositoryImpl(get(), get()) }
    single { DownloadManager(get(), get(), get(), get(), get()) }
    single<UploadRepository> { UploadRepositoryImpl(get()) }
    single { UploadManager(get(), get(), get(), get()) }
    single { ContextSelectionManager() }
}

val featureModule = module {
    viewModel { OnboardingViewModel(get()) }
    viewModel { CredentialsViewModel(get(), get(), get(), get()) }

    // Storage UseCases
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

    // Storage ViewModels
    viewModelOf(::BucketsViewModel)
    viewModel { (bucketId: String, path: String?) -> 
        FileBrowserViewModel(bucketId, path, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
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
            directoryPicker = get(),
            logger = get()
        )
    }
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UploadViewModel)
    viewModelOf(::StarredViewModel)
    viewModel { (bucketId: String?) ->
        SearchViewModel(bucketId, get(), get(), get())
    }
}