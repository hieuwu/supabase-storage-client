package com.hieuwu.supabasestorageclient.domain.model

data class UserSettings(
    val fileSizeLimit: Long,
    val fileSizeUnit: SizeUnit,
    val viewMode: ViewMode,
    val theme: AppTheme
)
