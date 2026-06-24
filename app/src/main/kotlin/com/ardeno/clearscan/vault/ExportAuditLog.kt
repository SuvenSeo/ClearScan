package com.ardeno.clearscan.vault

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

data class ExportAuditEntry(
    val timestamp: Instant,
    val documentId: String,
    val documentTitle: String,
    val exportKind: String
)

class ExportAuditLog(context: Context) {
    private val logFile = java.io.File(context.filesDir, "export-audit.json")

    fun record(documentId: String, documentTitle: String, exportKind: String) {
        val entries = readEntries().toMutableList()
        entries.add(
            0,
            ExportAuditEntry(
                timestamp = Instant.now(),
                documentId = documentId,
                documentTitle = documentTitle,
                exportKind = exportKind
            )
        )
        val trimmed = entries.take(MAX_ENTRIES)
        writeEntries(trimmed)
    }

    fun readEntries(): List<ExportAuditEntry> {
        if (!logFile.exists()) return emptyList()
        val raw = logFile.readText()
        if (raw.isBlank()) return emptyList()

        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ExportAuditEntry(
                        timestamp = Instant.parse(item.getString("timestamp")),
                        documentId = item.getString("documentId"),
                        documentTitle = item.getString("documentTitle"),
                        exportKind = item.getString("exportKind")
                    )
                )
            }
        }
    }

    fun formattedEntries(limit: Int = 20): List<String> =
        readEntries()
            .take(limit)
            .map { entry ->
                val whenLabel = formatter.format(entry.timestamp.atZone(ZoneId.systemDefault()))
                "$whenLabel · ${entry.documentTitle} (${entry.exportKind})"
            }

    private fun writeEntries(entries: List<ExportAuditEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("timestamp", entry.timestamp.toString())
                    .put("documentId", entry.documentId)
                    .put("documentTitle", entry.documentTitle)
                    .put("exportKind", entry.exportKind)
            )
        }
        logFile.writeText(array.toString(2))
    }

    private companion object {
        const val MAX_ENTRIES = 200
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    }
}
