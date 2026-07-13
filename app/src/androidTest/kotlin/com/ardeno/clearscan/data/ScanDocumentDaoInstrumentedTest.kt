package com.ardeno.clearscan.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ardeno.clearscan.data.db.ScanDatabase
import com.ardeno.clearscan.data.db.ScanDocumentDao
import com.ardeno.clearscan.data.db.toDocument
import com.ardeno.clearscan.data.db.toEntity
import com.ardeno.clearscan.model.ScanDocument
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanDocumentDaoInstrumentedTest {

    private lateinit var db: ScanDatabase
    private lateinit var dao: ScanDocumentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ScanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.scanDocumentDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndDeleteById_removesDocument() = runBlocking {
        val now = Instant.now()
        val doc = ScanDocument(
            id = "dao-delete-test",
            title = "Delete Me",
            pageCount = 1,
            createdAt = now,
            pdfPath = null,
            pageImagePaths = listOf("/pages/page1.jpg"),
            pageHashes = listOf("a1b2c3d4e5f67890", "f0987654321abcde")
        )

        dao.upsert(doc.toEntity())
        assertEquals(1, dao.count())
        assertEquals(doc.id, dao.getById(doc.id)?.id)

        dao.deleteById(doc.id)
        assertEquals(0, dao.count())
        assertNull(dao.getById(doc.id))
    }

    @Test
    fun upsert_persistsPageHashesInJsonPayload() = runBlocking {
        val now = Instant.now()
        val pageHashes = listOf("deadbeefcafebabe", "0123456789abcdef")
        val doc = ScanDocument(
            id = "dao-hashes-test",
            title = "Hashes",
            pageCount = 2,
            createdAt = now,
            pdfPath = null,
            pageImagePaths = listOf("/pages/p1.jpg", "/pages/p2.jpg"),
            pageHashes = pageHashes
        )

        dao.upsert(doc.toEntity())
        val stored = dao.getById(doc.id)!!
        assertEquals(pageHashes, stored.toDocument().pageHashes)
    }
}
