package com.hieuwu.supabasestorageclient.domain.model

data class Bucket(
    val id: String,
    val name: String,
    val owner: String,
    val public: Boolean,
    val createdAt: String,
    val updatedAt: String
)
