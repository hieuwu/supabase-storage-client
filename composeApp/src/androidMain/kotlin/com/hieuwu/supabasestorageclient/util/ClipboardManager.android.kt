package com.hieuwu.supabasestorageclient.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.koin.mp.KoinPlatformTools

class AndroidClipboardManager(private val context: Context) : com.hieuwu.supabasestorageclient.util.ClipboardManager {
    override fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Supabase Storage", text)
        clipboard.setPrimaryClip(clip)
    }
}

// Note: This is a hacky way to get context in a static expect function
// Ideally we register it in Koin
actual fun getClipboardManager(): com.hieuwu.supabasestorageclient.util.ClipboardManager {
    val context = KoinPlatformTools.defaultContext().get().get<Context>()
    return AndroidClipboardManager(context)
}
