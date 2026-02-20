package com.hieuwu.supabasestorageclient.util

import java.io.File
import java.io.FileOutputStream
import java.awt.Desktop
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmFileWriter : FileWriter {
    override fun writeToFile(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(data) }
    }

    override fun exists(path: String): Boolean {
        return File(path).exists()
    }
}

actual fun getFileWriter(): FileWriter = JvmFileWriter()

class JvmDirectoryPicker : DirectoryPicker {
    override suspend fun pickDirectory(): String? = withContext(Dispatchers.Main) {
        val challenger = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select directory to save file"
        }
        val result = challenger.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            challenger.selectedFile.absolutePath
        } else {
            null
        }
    }
}

actual fun getDirectoryPicker(): DirectoryPicker = JvmDirectoryPicker()

class JvmFileOpener : FileOpener {
    override fun openFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                // Open the directory and select the file if possible, or just open the folder
                // For simplicity, let's open the parent folder or the file itself
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.parentFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

actual fun getFileOpener(): FileOpener = JvmFileOpener()
