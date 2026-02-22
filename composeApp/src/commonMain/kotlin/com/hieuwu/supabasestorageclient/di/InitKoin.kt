package com.hieuwu.supabasestorageclient.di

import co.touchlab.kermit.Logger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    Logger.d { "Koin initialized" }
    modules(
        platformModule(),
        coreModule,
        featureModule
    )
}
