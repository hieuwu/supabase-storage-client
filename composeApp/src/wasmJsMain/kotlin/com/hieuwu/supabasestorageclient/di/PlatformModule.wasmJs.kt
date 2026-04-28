package com.hieuwu.supabasestorageclient.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<com.russhwolf.settings.Settings> { com.russhwolf.settings.StorageSettings() }
    single { com.hieuwu.supabasestorageclient.data.datasource.local.database.DatabaseDriverFactory() }
}
