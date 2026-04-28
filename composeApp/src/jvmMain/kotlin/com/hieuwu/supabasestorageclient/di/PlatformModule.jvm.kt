package com.hieuwu.supabasestorageclient.di

import org.koin.core.module.Module
import org.koin.dsl.module

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class JvmClipboardManager : com.hieuwu.supabasestorageclient.platform.ClipboardManager {
    override fun copyText(text: String) {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }
}

actual fun platformModule(): Module = module {
    single<com.russhwolf.settings.Settings> { com.russhwolf.settings.PreferencesSettings.Factory().create() }
    single<com.hieuwu.supabasestorageclient.platform.ClipboardManager> { JvmClipboardManager() }
    single { com.hieuwu.supabasestorageclient.database.DatabaseDriverFactory() }
}
