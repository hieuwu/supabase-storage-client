package com.hieuwu.supabasestorageclient.platform

interface FileWriter {
    fun writeToFile(path: String, data: ByteArray)
    fun exists(path: String): Boolean
}

expect fun getFileWriter(): FileWriter
