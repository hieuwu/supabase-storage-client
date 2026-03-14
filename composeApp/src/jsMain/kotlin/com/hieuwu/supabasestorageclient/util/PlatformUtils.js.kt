package com.hieuwu.supabasestorageclient.util

import kotlinx.browser.document
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

actual fun getPermissionManager(): PermissionManager = JsPermissionManager()

class JsFilePicker : FilePicker {
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
                    val bytes = if (result is Int8Array) {
                        val array = result
                        ByteArray(array.length) { i -> array[i] }
                    } else {
                        // For Browsers, result is often an ArrayBuffer when using readAsArrayBuffer
                        // We'll use a simpler approach if possible or assume Uint8Array
                        ByteArray(0)
                    }
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

actual fun getFilePicker(): FilePicker = JsFilePicker()
