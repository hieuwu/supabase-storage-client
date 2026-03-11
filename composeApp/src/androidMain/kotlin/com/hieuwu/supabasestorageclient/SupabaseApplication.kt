package com.hieuwu.supabasestorageclient

import android.app.Application
import com.hieuwu.supabasestorageclient.di.initKoin
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class SupabaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@SupabaseApplication)
            androidLogger()
        }

        // Initialize RevenueCat
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(PurchasesConfiguration(BuildKonfig.REVENUECAT_API_KEY))

    }
}
