package com.hieuwu.supabasestorageclient.util

interface DirectoryPicker {
    suspend fun pickDirectory(): String?
}

expect fun getDirectoryPicker(): DirectoryPicker
