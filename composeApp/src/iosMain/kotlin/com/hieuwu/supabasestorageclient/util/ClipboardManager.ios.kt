package com.hieuwu.supabasestorageclient.util

import platform.UIKit.UIPasteboard

class IosClipboardManager : ClipboardManager {
    override fun copyText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}

actual fun getClipboardManager(): ClipboardManager = IosClipboardManager()
