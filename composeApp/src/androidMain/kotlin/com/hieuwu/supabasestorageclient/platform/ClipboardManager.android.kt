package com.hieuwu.supabasestorageclient.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import co.touchlab.kermit.Logger
import org.koin.mp.KoinPlatformTools

private val clipboardLogger = Logger.withTag("ClipboardManager")

class AndroidClipboardManager(private val context: Context) : com.hieuwu.supabasestorageclient.platform.ClipboardManager {
    /**
     * Throws on failure so the caller can tell the user the copy did not happen - the clipboard
     * service can be unavailable, and `setPrimaryClip` can be rejected by the system.
     */
    override fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: run {
                clipboardLogger.e { "Clipboard service is unavailable" }
                error("Clipboard service is unavailable")
            }
        clipboard.setPrimaryClip(ClipData.newPlainText("Supabase Storage", text))
    }
}

// Note: This is a hacky way to get context in a static expect function
// Ideally we register it in Koin
actual fun getClipboardManager(): com.hieuwu.supabasestorageclient.platform.ClipboardManager {
    val context = KoinPlatformTools.defaultContext().get().get<Context>()
    return AndroidClipboardManager(context)
}
