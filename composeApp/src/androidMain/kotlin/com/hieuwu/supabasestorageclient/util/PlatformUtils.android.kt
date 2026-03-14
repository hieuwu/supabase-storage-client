package com.hieuwu.supabasestorageclient.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.mp.KoinPlatformTools
import java.io.File
import java.io.FileOutputStream

class AndroidFileWriter : FileWriter {
    override fun writeToFile(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(data) }
    }

    override fun exists(path: String): Boolean {
        return File(path).exists()
    }
}

actual fun getFileWriter(): FileWriter = AndroidFileWriter()
