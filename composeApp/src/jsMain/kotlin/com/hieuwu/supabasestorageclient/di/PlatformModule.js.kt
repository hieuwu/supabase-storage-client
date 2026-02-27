package com.hieuwu.supabasestorageclient.di

import org.koin.core.module.Module
import org.koin.dsl.module

import kotlinx.browser.window

class JsClipboardManager : com.hieuwu.supabasestorageclient.util.ClipboardManager {
    override fun copyText(text: String) {
        window.navigator.clipboard.writeText(text)
    }
}

actual fun platformModule(): Module = module {
    single<com.hieuwu.supabasestorageclient.util.ClipboardManager> { JsClipboardManager() }
    single { com.hieuwu.supabasestorageclient.database.DatabaseDriverFactory() }
}
