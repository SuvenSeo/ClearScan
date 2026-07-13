package com.ardeno.clearscan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempFile

class AppUpdateManifestParserTest {
    @Test
    fun parse_validManifest() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "0.2.1",
              "apkUrl": "https://github.com/SuvenSeo/ClearScan/releases/download/v0.2.1/app.apk",
              "releaseNotes": "Fixes",
              "minVersionCode": 2
            }
        """.trimIndent()

        val info = AppUpdateManifestParser.parse(json)

        assertEquals(3, info.versionCode)
        assertEquals("0.2.1", info.versionName)
        assertNull(info.sha256)
        assertTrue(info.isNewerThan(2))
        assertTrue(info.supportsInstalledVersion(2))
    }

    @Test
    fun parse_manifestWithSha256() {
        val sha256 = "a".repeat(64)
        val json = """
            {
              "versionCode": 4,
              "versionName": "0.2.2",
              "apkUrl": "https://example.com/app.apk",
              "sha256": "$sha256"
            }
        """.trimIndent()

        val info = AppUpdateManifestParser.parse(json)

        assertEquals(sha256, info.sha256)
    }

    @Test
    fun parse_blankSha256IsIgnored() {
        val json = """
            {
              "versionCode": 4,
              "versionName": "0.2.2",
              "apkUrl": "https://example.com/app.apk",
              "sha256": ""
            }
        """.trimIndent()

        val info = AppUpdateManifestParser.parse(json)

        assertNull(info.sha256)
    }

    @Test
    fun parse_invalidSha256Rejected() {
        val json = """
            {
              "versionCode": 4,
              "versionName": "0.2.2",
              "apkUrl": "https://example.com/app.apk",
              "sha256": "not-a-valid-hash"
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            AppUpdateManifestParser.parse(json)
        }
    }

    @Test
    fun apkIntegrityVerifier_computesSha256() {
        val tempFile = createTempFile(prefix = "clearscan-", suffix = ".apk").toFile()
        tempFile.writeText("clearscan-update-payload")

        val expected = ApkIntegrityVerifier.sha256Of(tempFile)

        assertEquals(64, expected.length)
        assertTrue(expected.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(ApkIntegrityVerifier.verify(tempFile, expected))
        assertTrue(ApkIntegrityVerifier.verify(tempFile, expected.uppercase()))
    }

    @Test
    fun apkIntegrityVerifier_skipsVerificationWhenExpectedMissing() {
        val tempFile = createTempFile(prefix = "clearscan-", suffix = ".apk").toFile()
        tempFile.writeText("clearscan-update-payload")

        assertTrue(ApkIntegrityVerifier.verify(tempFile, null))
        assertTrue(ApkIntegrityVerifier.verify(tempFile, ""))
        assertTrue(ApkIntegrityVerifier.verify(tempFile, "   "))
    }

    @Test
    fun apkIntegrityVerifier_detectsMismatch() {
        val tempFile = createTempFile(prefix = "clearscan-", suffix = ".apk").toFile()
        tempFile.writeText("clearscan-update-payload")

        val mismatch = "b".repeat(64)

        assertTrue(!ApkIntegrityVerifier.verify(tempFile, mismatch))
    }
}
