package com.ardeno.clearscan.model

import java.time.Instant

data class DocumentFolder(
    val id: String,
    val name: String,
    val createdAt: Instant
)
