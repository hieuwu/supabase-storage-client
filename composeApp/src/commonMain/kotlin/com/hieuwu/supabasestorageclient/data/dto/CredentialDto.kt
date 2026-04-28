package com.hieuwu.supabasestorageclient.data.dto

import com.hieuwu.supabasestorageclient.domain.model.Credential
import kotlinx.serialization.Serializable

@Serializable
data class CredentialDto(
    val id: String,
    val name: String,
    val supabaseUrl: String,
    val supabaseKey: String
)

fun CredentialDto.toDomain(): Credential = Credential(
    id = id,
    name = name,
    url = supabaseUrl,
    key = supabaseKey
)

fun Credential.toDto(): CredentialDto = CredentialDto(
    id = id,
    name = name,
    supabaseUrl = url,
    supabaseKey = key
)
