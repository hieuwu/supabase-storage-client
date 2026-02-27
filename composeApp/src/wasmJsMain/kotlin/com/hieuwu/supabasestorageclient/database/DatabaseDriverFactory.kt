package com.hieuwu.supabasestorageclient.database

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        throw UnsupportedOperationException("SQLDelight WasmJS driver requires web worker setup. Not implemented.")
    }
}
