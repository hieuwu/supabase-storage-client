package com.hieuwu.supabasestorageclient.di

import org.koin.core.context.startKoin

class KoinHelper {
    fun initKoin() {
        startKoin {
            modules(
                platformModule(),
                coreModule,
                featureModule
            )
        }
    }
}
