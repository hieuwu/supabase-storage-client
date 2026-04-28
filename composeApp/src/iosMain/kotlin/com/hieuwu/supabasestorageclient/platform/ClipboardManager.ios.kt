package com.hieuwu.supabasestorageclient.platform

import platform.UIKit.UIPasteboard

class IosClipboardManager : ClipboardManager {
    override fun copyText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}

actual fun getClipboardManager(): ClipboardManager = IosClipboardManager()
