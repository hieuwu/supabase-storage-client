package com.hieuwu.supabasestorageclient.data.datasource.local.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
