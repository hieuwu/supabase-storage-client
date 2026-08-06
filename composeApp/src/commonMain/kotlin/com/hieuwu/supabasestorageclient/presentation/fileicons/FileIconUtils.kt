package com.hieuwu.supabasestorageclient.presentation.fileicons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.graphics.vector.ImageVector
import com.hieuwu.supabasestorageclient.core.FileUtils

object FileIconUtils {
    fun getFileIcon(fileName: String): ImageVector {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when  {
            FileUtils.isPdf(extension) -> Icons.Default.Description
            FileUtils.isImage(extension) -> Icons.Default.Image
            FileUtils.isVideo(extension) -> Icons.Default.Movie
            else -> Icons.Default.InsertDriveFile
        }
    }
}