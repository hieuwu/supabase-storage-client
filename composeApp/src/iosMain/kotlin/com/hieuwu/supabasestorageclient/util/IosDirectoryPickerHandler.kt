package com.hieuwu.supabasestorageclient.util

import platform.UIKit.*
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosDirectoryPickerHandler : NSObject(), UIDocumentPickerDelegateProtocol {
    private var deferred: CompletableDeferred<String?>? = null

    @OptIn(ExperimentalForeignApi::class)
    suspend fun pickDirectory(): String? {
        println("IosDirectoryPickerHandler: pickDirectory called")
        if (deferred?.isActive == true) return null
        
        val def = CompletableDeferred<String?>()
        deferred = def

        kotlinx.coroutines.delay(100)

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val folderType = UTTypeFolder
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(folderType), asCopy = false)
                picker.delegate = this
                picker.allowsMultipleSelection = false
                picker.modalPresentationStyle = UIModalPresentationPageSheet

                val topViewController = getTopViewController()
                println("IosDirectoryPickerHandler: topViewController = $topViewController")
                
                if (topViewController == null) {
                    println("IosDirectoryPickerHandler: Error - topViewController is null")
                    deferred?.complete(null)
                } else {
                    println("IosDirectoryPickerHandler: calling presentViewController")
                    topViewController.presentViewController(picker, animated = true, completion = {
                        println("IosDirectoryPickerHandler: presentViewController completion block called")
                    })
                }
            } catch (e: Exception) {
                println("IosDirectoryPickerHandler: Error in dispatch_async - ${e.message}")
                deferred?.complete(null)
            }
        }

        val result = def.await()
        println("IosDirectoryPickerHandler: returning $result")
        return result
    }

    private fun getTopViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.windows.firstOrNull { (it as UIWindow).isKeyWindow() } as? UIWindow
            ?: UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        
        var top = window?.rootViewController
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url != null) {
            // Start accessing the security scoped resource
            val success = url.startAccessingSecurityScopedResource()
            if (success) {
                deferred?.complete(url.path)
            } else {
                deferred?.complete(null)
            }
        } else {
            deferred?.complete(null)
        }
        deferred = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        deferred?.complete(null)
        deferred = null
    }
}

object IosDirectoryPickerProvider {
    val handler = IosDirectoryPickerHandler()
}
