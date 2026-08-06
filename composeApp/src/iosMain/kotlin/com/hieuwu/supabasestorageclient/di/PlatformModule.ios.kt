package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.data.datasource.local.database.DatabaseDriverFactory
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<Settings> { com.russhwolf.settings.KeychainSettings(service = "SupabaseClient") }
    single<com.hieuwu.supabasestorageclient.platform.ClipboardManager> {
        com.hieuwu.supabasestorageclient.platform.IosClipboardManager()
    }
    single { DatabaseDriverFactory() }
}
