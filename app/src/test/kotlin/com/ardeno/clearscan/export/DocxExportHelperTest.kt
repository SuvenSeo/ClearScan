package com.ardeno.clearscan.export

import com.ardeno.clearscan.testing.RobolectricUnitTest
import java.util.zip.ZipFile
import kotlin.io.path.createTempFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocxExportHelperTest : RobolectricUnitTest() {
    @Test
    fun escapeXml_escapesReservedCharacters() {
        assertEquals("&amp;&lt;&gt;&quot;&apos;", DocxExportHelper.escapeXml("&<>\"'"))
    }

    @Test
    fun writeDocx_producesValidZipPackage() {
        val file = createTempFile(prefix = "clearscan-", suffix = ".docx").toFile()
        DocxExportHelper.writeDocx(
            file = file,
            title = "Invoice <A&B>",
            body = "Line 1\nLine 2"
        )

        ZipFile(file).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertTrue(names.contains("[Content_Types].xml"))
            assertTrue(names.contains("_rels/.rels"))
            assertTrue(names.contains("word/document.xml"))

            val documentXml = zip.getInputStream(zip.getEntry("word/document.xml"))
                .bufferedReader()
                .readText()
            assertTrue(documentXml.contains("Invoice &lt;A&amp;B&gt;"))
            assertTrue(documentXml.contains("Line 1"))
            assertTrue(documentXml.contains("Line 2"))
        }
    }
}
