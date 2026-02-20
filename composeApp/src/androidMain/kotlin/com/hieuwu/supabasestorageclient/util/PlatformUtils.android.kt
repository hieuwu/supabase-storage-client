package com.hieuwu.supabasestorageclient.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.mp.KoinPlatformTools
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

class AndroidFilePicker : FilePicker {
    private val context: Context
        get() = KoinPlatformTools.defaultContext().get().get<Context>()

    override suspend fun pickFile(): SelectedFile? {
        val uri = FilePickerHandler.pickFile() ?: return null
        val fileName = getFileName(context, uri) ?: "unknown_file"
        val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        return SelectedFile(fileName, data)
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}

actual fun getFilePicker(): FilePicker = AndroidFilePicker()

class AndroidFileOpener : FileOpener {
    private val context: Context
        get() = KoinPlatformTools.defaultContext().get().get<Context>()

    override fun openFile(path: String) {
        val file = File(path)
        if (file.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun openDirectory(path: String) {
        val file = File(path)
        val directory = if (file.isDirectory) file else file.parentFile
        if (directory != null && directory.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                directory
            )
            intent.setDataAndType(uri, "resource/folder")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: Just open the generic files app or downloads
            }
        }
    }
}

actual fun getFileOpener(): FileOpener = AndroidFileOpener()

class AndroidPermissionManager : PermissionManager {
    override suspend fun requestStoragePermission(): Boolean {
        // In a real app, this would request permissions via the Activity
        // For Android 11+ (Scoped Storage), we often don't need MANAGE_EXTERNAL_STORAGE 
        // if writing to app-specific or public Download folders via MediaStore/SAF.
        return true
    }
}

actual fun getPermissionManager(): PermissionManager = AndroidPermissionManager()
