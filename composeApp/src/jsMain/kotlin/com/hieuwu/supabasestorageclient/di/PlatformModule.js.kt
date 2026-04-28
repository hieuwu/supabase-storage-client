package com.hieuwu.supabasestorageclient.di

import org.koin.core.module.Module
import org.koin.dsl.module

import kotlinx.browser.window

class JsClipboardManager : com.hieuwu.supabasestorageclient.platform.ClipboardManager {
    override fun copyText(text: String) {
        window.navigator.clipboard.writeText(text)
    }
}

actual fun platformModule(): Module = module {
    single<com.russhwolf.settings.Settings> { com.russhwolf.settings.StorageSettings() }
    single<com.hieuwu.supabasestorageclient.platform.ClipboardManager> { JsClipboardManager() }
    single { com.hieuwu.supabasestorageclient.data.datasource.local.database.DatabaseDriverFactory() }
}
