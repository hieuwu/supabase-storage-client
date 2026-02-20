package com.hieuwu.supabasestorageclient.util

interface PermissionManager {
    suspend fun requestStoragePermission(): Boolean
}

expect fun getPermissionManager(): PermissionManager
