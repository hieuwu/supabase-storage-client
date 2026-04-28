package com.hieuwu.supabasestorageclient.platform

import platform.Foundation.*
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