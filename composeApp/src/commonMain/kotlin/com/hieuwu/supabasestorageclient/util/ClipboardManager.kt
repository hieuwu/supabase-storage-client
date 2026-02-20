package com.hieuwu.supabasestorageclient.util

interface ClipboardManager {
    fun copyText(text: String)
}

expect fun getClipboardManager(): ClipboardManager
