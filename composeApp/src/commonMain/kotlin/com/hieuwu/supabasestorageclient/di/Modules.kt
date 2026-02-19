package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.data.repository.CredentialRepositoryImpl
import com.hieuwu.supabasestorageclient.data.repository.OnboardingRepositoryImpl
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsViewModel
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

import com.hieuwu.supabasestorageclient.data.repository.StorageRepositoryImpl
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import com.hieuwu.supabasestorageclient.presentation.bucket.BucketViewModel
import com.hieuwu.supabasestorageclient.presentation.buckets.BucketsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    single { SupabaseClientManager() }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
    single<CredentialRepository> { CredentialRepositoryImpl(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get()) }
}

val featureModule = module {
    viewModel { OnboardingViewModel(get()) }
    viewModel { CredentialsViewModel(get(), get()) }
    
    // Storage UseCases
    singleOf(::GetBucketsUseCase)
    singleOf(::GetBucketContentsUseCase)
    
    // Storage ViewModels
    viewModelOf(::BucketsViewModel)
    viewModel { (bucketId: String, path: String?) -> BucketViewModel(bucketId, path, get()) }
}

