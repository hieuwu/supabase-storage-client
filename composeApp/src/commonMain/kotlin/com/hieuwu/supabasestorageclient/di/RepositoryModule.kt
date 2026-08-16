package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.data.datasource.LocalStorageDataSource
import com.hieuwu.supabasestorageclient.data.datasource.RemoteStorageDataSource
import com.hieuwu.supabasestorageclient.data.datasource.local.LocalStorageDataSourceImpl
import com.hieuwu.supabasestorageclient.data.datasource.remote.RemoteStorageDataSourceImpl
import com.hieuwu.supabasestorageclient.data.repository.*
import com.hieuwu.supabasestorageclient.domain.repository.*
import org.koin.dsl.module

val repositoryModule = module {
    single<RemoteStorageDataSource> { RemoteStorageDataSourceImpl(get(), get()) }
    single<LocalStorageDataSource> { LocalStorageDataSourceImpl(get()) }

    single<OnboardingRepository> { OnboardingRepositoryImpl(get(), get()) }
    single<CredentialRepository> { CredentialRepositoryImpl(get(), get()) }
    single<PurchaseRepository> { PurchaseRepositoryImpl(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get(), get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(get(), get()) }
    single<StarredRepository> { StarredRepositoryImpl(get(), get(), get()) }
    single<UploadRepository> { UploadRepositoryImpl(get(), get()) }
}
