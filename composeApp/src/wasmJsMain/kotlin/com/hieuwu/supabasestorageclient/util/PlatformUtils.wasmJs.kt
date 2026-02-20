package com.hieuwu.supabasestorageclient.util

class WasmFileWriter : FileWriter {
    override fun writeToFile(path: String, data: ByteArray) {
        // TODO: Implement browser file download
    }
    override fun exists(path: String): Boolean = false
}

actual fun getFileWriter(): FileWriter = WasmFileWriter()

class WasmDirectoryPicker : DirectoryPicker {
    override suspend fun pickDirectory(): String? = "downloads"
}

actual fun getDirectoryPicker(): DirectoryPicker = WasmDirectoryPicker()

class WasmFileOpener : FileOpener {
    override fun openFile(path: String) {}
}

actual fun getFileOpener(): FileOpener = WasmFileOpener()
