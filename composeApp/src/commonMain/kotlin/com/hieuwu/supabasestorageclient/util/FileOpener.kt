package com.hieuwu.supabasestorageclient.util

interface FileOpener {
    fun openFile(path: String)
    fun openDirectory(path: String)
}

expect fun getFileOpener(): FileOpener
