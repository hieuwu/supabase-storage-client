package com.hieuwu.supabasestorageclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import com.hieuwu.supabasestorageclient.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SupabaseStorageClient",
        ) {
            App()
        }
    }
}