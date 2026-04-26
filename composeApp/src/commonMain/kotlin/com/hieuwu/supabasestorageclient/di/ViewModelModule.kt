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
    viewModel { OnboardingViewModel(get()) }
    viewModel { CredentialsViewModel(get(), get(), get(), get(), get()) }

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
            clipboardManager = get(),
            downloadManager = get(),
            settingsRepository = get(),
            logger = get()
        )
    }
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UploadViewModel)
    viewModelOf(::StarredViewModel)
    viewModel { (bucketId: String?) ->
        SearchViewModel(bucketId, get(), get(), get(), get())
    }
}
