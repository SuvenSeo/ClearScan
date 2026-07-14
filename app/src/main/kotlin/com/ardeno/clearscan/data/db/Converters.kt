package com.ardeno.clearscan.data.db

import com.ardeno.clearscan.model.DocumentFolder
import com.ardeno.clearscan.model.OcrStatus
import com.ardeno.clearscan.model.PageAnnotationJson
import com.ardeno.clearscan.model.ReceiptFields
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.ScanMode
import com.ardeno.clearscan.ocr.OcrLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

fun ScanDocument.toEntity(): ScanDocumentEntity = ScanDocumentEntity(
    id = id,
    jsonPayload = scanDocumentToJsonPayload(this),
    updatedAt = updatedAt.toEpochMilli()
)

fun ScanDocumentEntity.toDocument(): ScanDocument {
    val json = JSONObject(jsonPayload)
    return ScanDocument(
        id = json.getString("id"),
        title = json.getString("title"),
        pageCount = json.getInt("pageCount"),
        createdAt = Instant.parse(json.getString("createdAt")),
        updatedAt = runCatching { Instant.parse(json.getString("updatedAt")) }
            .getOrDefault(Instant.parse(json.getString("createdAt"))),
        pdfPath = json.optString("pdfPath").takeUnless { it.isBlank() || it == "null" },
        searchablePdfPath = json.optString("searchablePdfPath").takeUnless { it.isBlank() || it == "null" },
        pageImagePaths = json.getJSONArray("pageImagePaths").toStringList(),
        tags = json.optJSONArray("tags")?.toStringList().orEmpty(),
        toolName = json.optString("toolName").takeUnless { it.isBlank() || it == "null" },
        sourceDocumentIds = json.optJSONArray("sourceDocumentIds")?.toStringList().orEmpty(),
        ocrText = json.optString("ocrText"),
        ocrStatus = runCatching { OcrStatus.valueOf(json.getString("ocrStatus")) }
            .getOrDefault(OcrStatus.NotStarted),
        searchablePdfReady = json.optBoolean("searchablePdfReady", false),
        ocrLanguage = OcrLanguage.fromName(json.optString("ocrLanguage")),
        scanMode = runCatching { ScanMode.valueOf(json.optString("scanMode", ScanMode.Document.name)) }
            .getOrDefault(ScanMode.Document),
        folderId = json.optString("folderId").takeUnless { it.isBlank() || it == "null" },
        isFavorite = json.optBoolean("isFavorite", false),
        pageHashes = json.optJSONArray("pageHashes")?.toStringList().orEmpty(),
        receiptFields = json.optJSONObject("receiptFields")?.let { it.toReceiptFields() },
        pageAnnotations = PageAnnotationJson.decodePages(
            json.optString("pageAnnotations")
                .takeUnless { it.isBlank() || it == "null" }
        ),
        deletedAt = json.optString("deletedAt")
            .takeUnless { it.isBlank() || it == "null" }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
    )
}

fun DocumentFolder.toEntity(): DocumentFolderEntity = DocumentFolderEntity(
    id = id,
    jsonPayload = documentFolderToJsonPayload(this),
    updatedAt = createdAt.toEpochMilli()
)

fun DocumentFolderEntity.toFolder(): DocumentFolder {
    val json = JSONObject(jsonPayload)
    return DocumentFolder(
        id = json.getString("id"),
        name = json.getString("name"),
        createdAt = Instant.parse(json.getString("createdAt"))
    )
}

private fun scanDocumentToJsonPayload(doc: ScanDocument): String = JSONObject()
    .put("id", doc.id)
    .put("title", doc.title)
    .put("pageCount", doc.pageCount)
    .put("createdAt", doc.createdAt.toString())
    .put("updatedAt", doc.updatedAt.toString())
    .put("pdfPath", doc.pdfPath)
    .put("searchablePdfPath", doc.searchablePdfPath)
    .put("pageImagePaths", JSONArray(doc.pageImagePaths))
    .put("tags", JSONArray(doc.tags))
    .put("toolName", doc.toolName)
    .put("sourceDocumentIds", JSONArray(doc.sourceDocumentIds))
    .put("ocrText", doc.ocrText)
    .put("ocrStatus", doc.ocrStatus.name)
    .put("searchablePdfReady", doc.searchablePdfReady)
    .put("ocrLanguage", doc.ocrLanguage.name)
    .put("scanMode", doc.scanMode.name)
    .put("folderId", doc.folderId)
    .put("isFavorite", doc.isFavorite)
    .put("pageHashes", JSONArray(doc.pageHashes))
    .put("receiptFields", doc.receiptFields?.let { receiptFieldsToJson(it) })
    .put("pageAnnotations", JSONArray(PageAnnotationJson.encodePages(doc.pageAnnotations)))
    .put("deletedAt", doc.deletedAt?.toString())
    .toString()

private fun documentFolderToJsonPayload(folder: DocumentFolder): String = JSONObject()
    .put("id", folder.id)
    .put("name", folder.name)
    .put("createdAt", folder.createdAt.toString())
    .toString()

private fun receiptFieldsToJson(fields: ReceiptFields): JSONObject = JSONObject()
    .put("merchant", fields.merchant)
    .put("amount", fields.amount)
    .put("date", fields.date)

private fun JSONObject.toReceiptFields(): ReceiptFields = ReceiptFields(
    merchant = optString("merchant").takeUnless { it.isBlank() || it == "null" },
    amount = optString("amount").takeUnless { it.isBlank() || it == "null" },
    date = optString("date").takeUnless { it.isBlank() || it == "null" }
)

private fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) {
        add(getString(index))
    }
}
