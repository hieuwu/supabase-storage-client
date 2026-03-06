package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import org.koin.dsl.module

val supabaseModule = module {
    single { SupabaseClientManager() }
}
