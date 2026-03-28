package com.hieuwu.supabasestorageclient

import android.app.Application
import com.hieuwu.supabasestorageclient.di.initKoin
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class SupabaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@SupabaseApplication)
            androidLogger()
        }
        FileKit.manualFileKitCoreInitialization(this)
    }
}
