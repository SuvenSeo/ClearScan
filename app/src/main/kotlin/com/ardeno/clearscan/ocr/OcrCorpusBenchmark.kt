package com.ardeno.clearscan.ocr

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

data class OcrCorpusEntry(
    val id: String,
    val language: BenchmarkLanguage,
    val category: String,
    val description: String,
    val expectedText: String,
    val actualText: String?,
    val imageFile: String?
)

object OcrCorpusBenchmark {
    private const val CORPUS_RESOURCE_DIR = "ocr-corpus"
    private const val INDEX_FILE = "index.json"

    fun loadFromClasspath(
        classLoader: ClassLoader,
        resourceDir: String = CORPUS_RESOURCE_DIR
    ): List<OcrCorpusEntry> {
        val indexStream = classLoader.getResourceAsStream("$resourceDir/$INDEX_FILE") ?: return emptyList()
        val index = JSONObject(indexStream.use(InputStream::readBytes).decodeToString())
        val entryFiles = index.optJSONArray("entries") ?: JSONArray()

        return buildList {
            for (index in 0 until entryFiles.length()) {
                val entryFile = entryFiles.getString(index)
                val entryStream = classLoader.getResourceAsStream("$resourceDir/$entryFile") ?: continue
                add(parseEntry(JSONObject(entryStream.use(InputStream::readBytes).decodeToString())))
            }
        }
    }

    fun evaluate(entries: List<OcrCorpusEntry>): List<OcrBenchmarkMetrics> =
        entries.mapNotNull { entry ->
            val actualText = entry.actualText ?: return@mapNotNull null
            OcrBenchmark.evaluate(
                OcrBenchmarkCase(
                    language = entry.language,
                    sampleName = entry.id,
                    expectedText = entry.expectedText,
                    actualText = actualText
                )
            )
        }

    fun evaluateClasspathCorpus(
        classLoader: ClassLoader,
        resourceDir: String = CORPUS_RESOURCE_DIR
    ): List<OcrBenchmarkMetrics> = evaluate(loadFromClasspath(classLoader, resourceDir))

    fun summary(metrics: List<OcrBenchmarkMetrics>): String = OcrBenchmark.summary(metrics)

    private fun parseEntry(json: JSONObject): OcrCorpusEntry {
        val language = when (json.getString("language").lowercase()) {
            "sinhala" -> BenchmarkLanguage.Sinhala
            "tamil" -> BenchmarkLanguage.Tamil
            else -> error("Unsupported corpus language: ${json.getString("language")}")
        }

        return OcrCorpusEntry(
            id = json.getString("id"),
            language = language,
            category = json.optString("category", "uncategorized"),
            description = json.optString("description", ""),
            expectedText = json.getString("expectedText"),
            actualText = json.optString("actualText").takeIf { json.has("actualText") && !json.isNull("actualText") },
            imageFile = json.optString("imageFile").takeIf { json.has("imageFile") && !json.isNull("imageFile") }
        )
    }
}
