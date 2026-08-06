package com.hieuwu.supabasestorageclient.di

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
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { 
        CredentialsViewModel(
            observeCredentialsUseCase = get(),
            getLastUsedCredentialIdUseCase = get(),
            addCredentialUseCase = get(),
            updateCredentialUseCase = get(),
            deleteCredentialUseCase = get(),
            triggerPaywallUseCase = get(),
            switchCredentialUseCase = get(),
            observeProStatusUseCase = get(),
            supabaseClientManager = get(),
            logger = get()
        )
    }

    viewModelOf(::BucketsViewModel)
    viewModel { (bucketId: String, path: String?) ->
        FileBrowserViewModel(
            bucketId,
            path,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { (bucketId: String, fileName: String, path: String?) ->
        FileViewViewModel(
            bucketId = bucketId,
            fileName = fileName,
            path = path,
            getPublicUrlUseCase = get(),
            deleteFileUseCase = get(),
            getFileMetadataUseCase = get(),
            getUserSettingsUseCase = get(),
            updateUserSettingsUseCase = get(),
            downloadFileUseCase = get(),
            clipboardManager = get(),
            logger = get()
        )
    }
    viewModel {
        DownloadsViewModel(
            observeDownloadsUseCase = get(),
            cancelDownloadUseCase = get(),
            deleteDownloadUseCase = get(),
            fileWriter = get()
        )
    }
    viewModel {
        MainViewModel(
            contextSelectionManager = get(),
            createFolderUseCase = get(),
            uploadFileUseCase = get(),
            createBucketUseCase = get(),
            updateBucketUseCase = get(),
            observeCredentialsUseCase = get(),
            getLastUsedCredentialIdUseCase = get(),
            observeUserSettingsUseCase = get(),
            updateUserSettingsUseCase = get(),
            switchCredentialUseCase = get(),
            deleteCredentialUseCase = get(),
            triggerPaywallUseCase = get(),
            observeProStatusUseCase = get(),
            restorePurchasesUseCase = get(),
            refreshManager = get(),
        )
    }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UploadViewModel)
    viewModelOf(::StarredViewModel)
    viewModel { (bucketId: String?) ->
        SearchViewModel(
            bucketId = bucketId,
            getBucketsUseCase = get(),
            getBucketContentsUseCase = get(),
            getStarredItemsUseCase = get(),
            toggleStarUseCase = get(),
            logger = get()
        )
    }
}
