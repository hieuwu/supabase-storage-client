package com.hieuwu.supabasestorageclient.util

import platform.Foundation.*
import platform.UIKit.*
import kotlinx.cinterop.*

class IosFileWriter : FileWriter {
    @OptIn(ExperimentalForeignApi::class)
    override fun writeToFile(path: String, data: ByteArray) {
        val nsData = data.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = data.size.toULong()
            )
        }
        nsData.writeToFile(path, true)
    }

    override fun exists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}

actual fun getFileWriter(): FileWriter = IosFileWriter()

class IosDirectoryPicker : DirectoryPicker {
    override suspend fun pickDirectory(): String? {
        // Use Documents directory as the default download location
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String
    }
}

actual fun getDirectoryPicker(): DirectoryPicker = IosDirectoryPicker()

class IosFileOpener : FileOpener {
    override fun openFile(path: String) {
        val url = NSURL.fileURLWithPath(path)
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun openDirectory(path: String) {
        val isDir = memScoped {
            val isDirectory = alloc<BooleanVar>()
            NSFileManager.defaultManager.fileExistsAtPath(path, isDirectory.ptr)
            isDirectory.value
        }
        
        val dirPath = if (isDir) path else (path.substringBeforeLast("/") + "/")
        val url = NSURL.fileURLWithPath(dirPath, isDirectory = true)
        
        // Use shareddocuments:// schema to try and open the Files app specifically
        // If that fails, fallback to file:// which might just open the file directly 
        // depending on iOS versions
        val stringUrl = url.absoluteString ?: ""
        if (stringUrl.startsWith("file://")) {
            val sharedDocsUrl = NSURL.URLWithString(stringUrl.replaceFirst("file://", "shareddocuments://"))
            if (sharedDocsUrl != null && UIApplication.sharedApplication.canOpenURL(sharedDocsUrl)) {
                UIApplication.sharedApplication.openURL(sharedDocsUrl, emptyMap<Any?, Any?>(), null)
                return
            }
        }
        
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }
}

actual fun getFileOpener(): FileOpener = IosFileOpener()

class IosPermissionManager : PermissionManager {
    override suspend fun requestStoragePermission(): Boolean = true
}

actual fun getPermissionManager(): PermissionManager = IosPermissionManager()

class IosFilePicker : FilePicker {
    override suspend fun pickFile(): SelectedFile? {
        return IosFilePickerProvider.handler.pickFile()
    }
}

actual fun getFilePicker(): FilePicker = IosFilePicker()
