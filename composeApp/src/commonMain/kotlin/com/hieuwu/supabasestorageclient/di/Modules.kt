package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.data.repository.OnboardingRepositoryImpl
import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    single { SupabaseClientManager(get()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
}

val featureModule = module {
    viewModel { OnboardingViewModel(get()) }
}
