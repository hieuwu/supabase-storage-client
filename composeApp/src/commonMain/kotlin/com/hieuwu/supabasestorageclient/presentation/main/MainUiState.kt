package com.hieuwu.supabasestorageclient.presentation.main

import com.hieuwu.supabasestorageclient.domain.model.Credential
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

data class MainUiState(
    val isNewFolderDialogVisible: Boolean = false,
    val newFolderName: String = "",
    val isCreateBucketDialogVisible: Boolean = false,
    val newBucketId: String = "",
    val isNewBucketPublic: Boolean = true,
    val isNewBucketSizeLimitEnabled: Boolean = false,
    val newBucketFileSizeLimit: String = "",
    val newBucketFileSizeUnit: SizeUnit = SizeUnit.MEGABYTES,
    val error: String? = null,
    val successMessage: String? = null,
    val isUploading: Boolean = false,
    val isCredentialsSheetVisible: Boolean = false,
    val credentials: List<Credential> = emptyList(),
    val lastUsedId: String? = null,
    val showCredentialSwitchConfirmation: Boolean = false,
    val selectedCredentialForSwitch: Credential? = null,
    val isSettingUpCredential: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val isPremium: Boolean = false
)