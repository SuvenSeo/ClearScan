package com.ardeno.clearscan.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<ScanDocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ScanDocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): ScanDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScanDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ScanDocumentEntity>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int
}
