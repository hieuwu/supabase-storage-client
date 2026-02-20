package com.hieuwu.supabasestorageclient.util

import platform.Foundation.*

class IosFileWriter : FileWriter {
    override fun writeToFile(path: String, data: ByteArray) {
        val nsData = NSData.create(
            bytes = data.toCValues(),
            length = data.size.toULong()
        )
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
        // On iOS, opening files in a file explorer requires UIDocumentInteractionController
        // This is a placeholder; a full implementation would require a UIViewController reference
    }

    override fun openDirectory(path: String) {
        // iOS doesn't have a direct "open directory" intent like desktop, 
        // but you can point to the Files app if integrated.
    }
}

actual fun getFileOpener(): FileOpener = IosFileOpener()

class IosPermissionManager : PermissionManager {
    override suspend fun requestStoragePermission(): Boolean = true
}

actual fun getPermissionManager(): PermissionManager = IosPermissionManager()
