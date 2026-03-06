package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.data.repository.*
import com.hieuwu.supabasestorageclient.domain.repository.*
import org.koin.dsl.module

val repositoryModule = module {
    single<OnboardingRepository> { OnboardingRepositoryImpl(get(), get()) }
    single<CredentialRepository> { CredentialRepositoryImpl(get(), get()) }
    single<PurchaseRepository> { PurchaseRepositoryImpl() }
    single<StorageRepository> { StorageRepositoryImpl(get(), get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(get()) }
    single<StarredRepository> { StarredRepositoryImpl(get(), get()) }
    single<UploadRepository> { UploadRepositoryImpl(get()) }
}
