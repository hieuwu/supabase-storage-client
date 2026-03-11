package com.hieuwu.supabasestorageclient.presentation.credentials

import com.hieuwu.supabasestorageclient.domain.model.Credential

data class CredentialsUiState(
    val credentials: List<Credential> = emptyList(),
    val lastUsedId: String? = null,
    val showCredentialsId: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSettingUp: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val credentialToDelete: Credential? = null,
    val showEditSheet: Boolean = false,
    val credentialToEdit: Credential? = null,
    val isPro: Boolean = false,
    val showAddSheet: Boolean = false
)