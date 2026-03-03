package com.hieuwu.supabasestorageclient.util

import platform.UIKit.*
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

class IosFilePickerHandler : NSObject(), UIDocumentPickerDelegateProtocol {
    private var deferred: CompletableDeferred<SelectedFile?>? = null

    @OptIn(ExperimentalForeignApi::class)
    suspend fun pickFile(): SelectedFile? {
        println("IosFilePickerHandler: pickFile called")
        if (deferred?.isActive == true) {
            println("IosFilePickerHandler: active session already exists, canceling")
            return null
        }
        
        val def = CompletableDeferred<SelectedFile?>()
        deferred = def

        // Give UIKit/Compose a moment to settle (e.g. if a bottom sheet is dismissing)
        kotlinx.coroutines.delay(100)

        dispatch_async(dispatch_get_main_queue()) {
            try {
                val contentTypes = listOf(
                    UTTypeItem,
                    UTTypeData,
                    UTTypeContent
                )
                
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes, asCopy = true)
                picker.delegate = this
                picker.allowsMultipleSelection = false
                picker.modalPresentationStyle = UIModalPresentationPageSheet

                val topViewController = getTopViewController()
                println("IosFilePickerHandler: topViewController = $topViewController")
                
                if (topViewController == null) {
                    println("IosFilePickerHandler: Error - topViewController is null")
                    deferred?.complete(null)
                } else {
                    println("IosFilePickerHandler: calling presentViewController")
                    topViewController.presentViewController(picker, animated = true, completion = {
                        println("IosFilePickerHandler: presentViewController completion block called")
                    })
                }
            } catch (e: Exception) {
                println("IosFilePickerHandler: Error in dispatch_async - ${e.message}")
                deferred?.complete(null)
            }
        }

        val result = def.await()
        println("IosFilePickerHandler: pickFile returning ${result?.name}")
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

    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        println("IosFilePickerHandler: documentPicker - didPickDocumentsAtURLs size: ${didPickDocumentsAtURLs.size}")
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        println("IosFilePickerHandler: documentPicker - url: $url")
        if (url != null) {
            val success = url.startAccessingSecurityScopedResource()
            println("IosFilePickerHandler: startAccessingSecurityScopedResource success: $success")
            try {
                val data = NSData.dataWithContentsOfURL(url)
                println("IosFilePickerHandler: data length: ${data?.length}")
                if (data != null) {
                    val byteArray = ByteArray(data.length.toInt()).apply {
                        usePinned { pinned ->
                            memcpy(pinned.addressOf(0), data.bytes, data.length)
                        }
                    }
                    deferred?.complete(SelectedFile(url.lastPathComponent ?: "unknown", byteArray))
                    println("IosFilePickerHandler: file picked successfully")
                } else {
                    println("IosFilePickerHandler: Error - data is null")
                    deferred?.complete(null)
                }
            } catch (e: Exception) {
                println("IosFilePickerHandler: Exception - ${e.message}")
                deferred?.complete(null)
            } finally {
                if (success) {
                    url.stopAccessingSecurityScopedResource()
                }
            }
        } else {
            println("IosFilePickerHandler: Error - url is null")
            deferred?.complete(null)
        }
        deferred = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        println("IosFilePickerHandler: documentPickerWasCancelled")
        deferred?.complete(null)
        deferred = null
    }
}

object IosFilePickerProvider {
    val handler = IosFilePickerHandler()
}
