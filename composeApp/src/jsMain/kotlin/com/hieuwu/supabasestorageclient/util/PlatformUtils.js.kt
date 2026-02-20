package com.hieuwu.supabasestorageclient.util

class JsFileWriter : FileWriter {
    override fun writeToFile(path: String, data: ByteArray) {
        // TODO: Implement browser file download via Blob
    }
    override fun exists(path: String): Boolean = false
}

actual fun getFileWriter(): FileWriter = JsFileWriter()

class JsDirectoryPicker : DirectoryPicker {
    override suspend fun pickDirectory(): String? = "downloads"
}

actual fun getDirectoryPicker(): DirectoryPicker = JsDirectoryPicker()

class JsFileOpener : FileOpener {
    override fun openFile(path: String) {}
}

actual fun getFileOpener(): FileOpener = JsFileOpener()
