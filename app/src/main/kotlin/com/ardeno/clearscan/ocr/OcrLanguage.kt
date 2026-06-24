package com.ardeno.clearscan.ocr

enum class OcrLanguage(
    val label: String,
    val tessCode: String?
) {
    Latin("Latin / English", null),
    Sinhala("Sinhala", "sin"),
    Tamil("Tamil", "tam");

    val usesTesseract: Boolean
        get() = tessCode != null

    fun toBenchmarkLanguage(): BenchmarkLanguage? = when (this) {
        Sinhala -> BenchmarkLanguage.Sinhala
        Tamil -> BenchmarkLanguage.Tamil
        Latin -> null
    }

    companion object {
        fun fromName(name: String?): OcrLanguage = entries.firstOrNull { it.name == name } ?: Latin
    }
}
