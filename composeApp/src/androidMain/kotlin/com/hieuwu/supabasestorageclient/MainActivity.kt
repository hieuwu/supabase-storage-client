package com.hieuwu.supabasestorageclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.hieuwu.supabasestorageclient.util.FilePickerHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    private val getFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        FilePickerHandler.onResult(uri)
    }

    private val getDirectory = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        com.hieuwu.supabasestorageclient.util.DirectoryPickerHandler.onResult(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FilePickerHandler.triggerPicker = {
            getFile.launch("*/*")
        }

        com.hieuwu.supabasestorageclient.util.DirectoryPickerHandler.triggerPicker = {
            getDirectory.launch(null)
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}