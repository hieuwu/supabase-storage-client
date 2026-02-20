package com.hieuwu.supabasestorageclient.util

import android.net.Uri
import kotlinx.coroutines.CompletableDeferred

object FilePickerHandler {
    private var deferred: CompletableDeferred<Uri?>? = null
    var triggerPicker: (() -> Unit)? = null

    suspend fun pickFile(): Uri? {
        val def = CompletableDeferred<Uri?>()
        deferred = def
        triggerPicker?.invoke()
        return def.await()
    }

    fun onResult(uri: Uri?) {
        deferred?.complete(uri)
        deferred = null
    }
}
