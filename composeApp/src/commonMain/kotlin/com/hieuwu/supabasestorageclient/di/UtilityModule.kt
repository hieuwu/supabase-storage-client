package com.hieuwu.supabasestorageclient.di

import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import com.hieuwu.supabasestorageclient.domain.RefreshManager
import org.koin.dsl.module

val utilityModule = module {
    single { Logger.withTag("SupabaseStorageClient") }
    single { DownloadManager(get(), get(), get(), get(), get(), get()) }
    single { UploadManager(get(), get(), get(), get(), get(), get()) }
    single { ContextSelectionManager() }
    single { RefreshManager() }
}
