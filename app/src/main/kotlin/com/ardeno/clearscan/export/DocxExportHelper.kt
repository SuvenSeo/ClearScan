package com.ardeno.clearscan.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ardeno.clearscan.model.ScanDocument
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Local, dependency-free OCR → DOCX export (Open XML package).
 * Not feature-parity with CamScanner cloud PDF→Word conversion — converts recognized text only.
 */
object DocxExportHelper {
    fun writeDocxFile(context: Context, document: ScanDocument): File? {
        val text = document.ocrText.trim()
        if (text.isBlank()) return null

        val cacheDir = File(context.cacheDir, "docx-exports").apply { mkdirs() }
        val safeTitle = document.title
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "scan" }
        val file = File(cacheDir, "$safeTitle-${Instant.now().toEpochMilli()}.docx")
        writeDocx(file, title = document.title, body = text)
        return file
    }

    fun createShareIntent(context: Context, document: ScanDocument): Intent? {
        val file = writeDocxFile(context, document) ?: return null
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun writeDocx(file: File, title: String, body: String) {
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
            writeEntry(zip, "_rels/.rels", ROOT_RELS)
            writeEntry(zip, "word/_rels/document.xml.rels", DOCUMENT_RELS)
            writeEntry(zip, "word/document.xml", documentXml(title = title, body = body))
        }
    }

    internal fun documentXml(title: String, body: String): String {
        val paragraphs = body
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .joinToString(separator = "") { line ->
                """
                <w:p>
                  <w:r>
                    <w:t xml:space="preserve">${escapeXml(line)}</w:t>
                  </w:r>
                </w:p>
                """.trimIndent()
            }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p>
                  <w:r>
                    <w:rPr><w:b/></w:rPr>
                    <w:t xml:space="preserve">${escapeXml(title)}</w:t>
                  </w:r>
                </w:p>
                $paragraphs
              </w:body>
            </w:document>
        """.trimIndent()
    }

    internal fun escapeXml(value: String): String =
        buildString(value.length) {
            for (ch in value) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private val CONTENT_TYPES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
        </Types>
    """.trimIndent()

    private val ROOT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()

    private val DOCUMENT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        </Relationships>
    """.trimIndent()
}
