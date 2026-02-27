package com.hieuwu.supabasestorageclient.di

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Context is provided by androidContext() in SupabaseApplication
    single<com.russhwolf.settings.Settings> {
        val context = get<Context>()
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        com.russhwolf.settings.SharedPreferencesSettings(sharedPreferences)
    }
    single<com.hieuwu.supabasestorageclient.util.ClipboardManager> { 
        com.hieuwu.supabasestorageclient.util.AndroidClipboardManager(get()) 
    }
    single { com.hieuwu.supabasestorageclient.database.DatabaseDriverFactory(get()) }
}
