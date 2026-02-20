package com.hieuwu.supabasestorageclient.util

data class SelectedFile(
    val name: String,
    val data: ByteArray
)

interface FilePicker {
    suspend fun pickFile(): SelectedFile?
}

expect fun getFilePicker(): FilePicker
