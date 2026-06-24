package com.ardeno.clearscan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(info.isNewerThan(2))
        assertTrue(info.supportsInstalledVersion(2))
    }
}
