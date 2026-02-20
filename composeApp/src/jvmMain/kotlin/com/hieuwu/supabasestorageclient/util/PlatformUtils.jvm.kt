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
        val file = java.io.File(path)
        if (file.exists()) {
            java.awt.Desktop.getDesktop().open(file)
        }
    }

    override fun openDirectory(path: String) {
        val file = java.io.File(path)
        if (file.exists()) {
            val directory = if (file.isDirectory) file else file.parentFile
            if (directory != null && directory.exists()) {
                java.awt.Desktop.getDesktop().open(directory)
            }
        }
    }
}

actual fun getFileOpener(): FileOpener = JvmFileOpener()

class JvmPermissionManager : PermissionManager {
    override suspend fun requestStoragePermission(): Boolean = true
}

actual fun getPermissionManager(): PermissionManager = JvmPermissionManager()

class JvmFilePicker : FilePicker {
    override suspend fun pickFile(): SelectedFile? = withContext(Dispatchers.Main) {
        val challenger = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Select file to upload"
        }
        val result = challenger.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = challenger.selectedFile
            SelectedFile(file.name, file.readBytes())
        } else {
            null
        }
    }
}

actual fun getFilePicker(): FilePicker = JvmFilePicker()
