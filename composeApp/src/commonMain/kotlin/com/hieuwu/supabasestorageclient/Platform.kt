package com.hieuwu.supabasestorageclient

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform