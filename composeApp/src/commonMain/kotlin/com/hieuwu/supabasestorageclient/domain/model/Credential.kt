package com.hieuwu.supabasestorageclient.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Credential(
    val id: String,
    val name: String,
    val url: String,
    val key: String
)
