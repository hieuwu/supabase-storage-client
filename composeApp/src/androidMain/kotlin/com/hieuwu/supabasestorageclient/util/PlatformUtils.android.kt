package com.hieuwu.supabasestorageclient.util

import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class AndroidFileWriter : FileWriter {
    override fun writeToFile(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(data) }
    }

    override fun exists(path: String): Boolean {
        return File(path).exists()
    }
}

actual fun getFileWriter(): FileWriter = AndroidFileWriter()

class AndroidDirectoryPicker : DirectoryPicker {
    override suspend fun pickDirectory(): String? {
        // In a real Android app, this would involve starting an Activity for result.
        // For this demo/implementation, we'll return a placeholder or use external storage.
        // A proper implementation would require access to activity/context which is usually
        // handled via a platform-specific bridge or a library like FileKit.
        return "/storage/emulated/0/Download" 
    }
}

actual fun getDirectoryPicker(): DirectoryPicker = AndroidDirectoryPicker()

class AndroidFileOpener : FileOpener {
    override fun openFile(path: String) {
        // Logic to open file in Android using Intent
    }
}

actual fun getFileOpener(): FileOpener = AndroidFileOpener()
