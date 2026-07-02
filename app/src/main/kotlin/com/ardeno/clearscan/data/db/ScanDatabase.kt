package com.ardeno.clearscan.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanDocumentEntity::class, DocumentFolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDocumentDao(): ScanDocumentDao
    abstract fun documentFolderDao(): DocumentFolderDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getInstance(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "clearscan.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
