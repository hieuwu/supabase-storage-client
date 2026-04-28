package com.hieuwu.supabasestorageclient.platform

interface ClipboardManager {
    fun copyText(text: String)
}
expect fun getClipboardManager(): ClipboardManager
