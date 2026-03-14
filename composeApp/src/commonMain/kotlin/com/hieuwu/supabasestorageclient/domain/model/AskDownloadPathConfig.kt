package com.hieuwu.supabasestorageclient.domain.model

enum class AskDownloadPathConfig(val label: String) {
    ASK_EVERYTIME("Ask everytime"),
    ONCE_WHEN_APP_OPEN("Once when app open"),
    NEVER_ASK("Never ask")
}
