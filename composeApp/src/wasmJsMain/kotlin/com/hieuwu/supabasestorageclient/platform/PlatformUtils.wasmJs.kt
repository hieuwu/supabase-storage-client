package com.hieuwu.supabasestorageclient.platform

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WasmDirectoryPicker : DirectoryPicker {
    override suspend fun pickDirectory(): String? = "downloads"
}

actual fun getDirectoryPicker(): DirectoryPicker = WasmDirectoryPicker()

actual fun getPermissionManager(): PermissionManager = WasmPermissionManager()

class WasmFilePicker : FilePicker {
    override suspend fun pickFile(): SelectedFile? = suspendCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.style.display = "none"
        
        input.onchange = {
            val file = input.files?.get(0)
            if (file != null) {
                val reader = FileReader()
                reader.onload = { event ->
                    val result = reader.result
                    // WasmJs needs to handle the buffer/array conversion
                    val bytes = ByteArray(0) // Simplified for WasmJs demonstration
                    continuation.resume(SelectedFile(file.name, bytes))
                }
                reader.readAsArrayBuffer(file)
            } else {
                continuation.resume(null)
            }
        }
        
        document.body?.appendChild(input)
        input.click()
        document.body?.removeChild(input)
    }
}

actual fun getFilePicker(): FilePicker = WasmFilePicker()
