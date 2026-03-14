package com.hieuwu.supabasestorageclient

import android.app.Application
import com.hieuwu.supabasestorageclient.di.initKoin
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
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

        try {
            // Initialize RevenueCat
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(PurchasesConfiguration(BuildKonfig.REVENUECAT_API_KEY))

        } catch (e: Exception) {
            print(e.message)
        }
    }
}
