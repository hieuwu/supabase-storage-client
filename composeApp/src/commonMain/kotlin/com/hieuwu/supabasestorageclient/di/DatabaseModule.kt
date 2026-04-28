package com.hieuwu.supabasestorageclient.di

import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.data.datasource.local.database.DatabaseDriverFactory
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
