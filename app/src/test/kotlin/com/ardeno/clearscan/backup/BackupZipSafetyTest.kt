package com.ardeno.clearscan.backup

import com.ardeno.clearscan.testing.RobolectricUnitTest

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupZipSafetyTest : RobolectricUnitTest() {
    private lateinit var targetDir: File

    @Before
    fun setUp() {
        targetDir = createTempDirectory("backup-zip-safety-").toFile()
    }

    @After
    fun tearDown() {
        targetDir.deleteRecursively()
    }

    @Test
    fun safeZipEntryPath_allowsNormalRelativePath() {
        val result = BackupRestoreManager.safeZipEntryPath(targetDir, "index.json")

        assertEquals(File(targetDir, "index.json").canonicalFile, result.canonicalFile)
    }

    @Test
    fun safeZipEntryPath_allowsNestedPath() {
        val result = BackupRestoreManager.safeZipEntryPath(targetDir, "docs/page1/image.jpg")

        assertTrue(result.canonicalFile.path.startsWith(targetDir.canonicalFile.path))
    }

    @Test(expected = SecurityException::class)
    fun safeZipEntryPath_rejectsParentTraversal() {
        BackupRestoreManager.safeZipEntryPath(targetDir, "../evil.txt")
    }

    @Test(expected = SecurityException::class)
    fun safeZipEntryPath_rejectsEmbeddedTraversal() {
        BackupRestoreManager.safeZipEntryPath(targetDir, "foo/../../etc/passwd")
    }

    @Test(expected = SecurityException::class)
    fun safeZipEntryPath_rejectsAbsolutePath() {
        BackupRestoreManager.safeZipEntryPath(targetDir, "/etc/passwd")
    }
}
