package com.ardeno.clearscan.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class DocumentFolderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "json_payload") val jsonPayload: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
