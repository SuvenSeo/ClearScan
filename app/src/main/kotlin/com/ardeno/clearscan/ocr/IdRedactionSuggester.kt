package com.ardeno.clearscan.ocr

data class IdRedactionSuggestion(
    val labels: List<String>,
    val regions: List<NormalizedRegion>
)

data class NormalizedRegion(
    val label: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toPixelRect(width: Int, height: Int): android.graphics.Rect {
        val margin = width.coerceAtMost(height) / 40f
        return android.graphics.Rect(
            (left * width - margin).toInt().coerceAtLeast(0),
            (top * height - margin).toInt().coerceAtLeast(0),
            (right * width + margin).toInt().coerceAtMost(width),
            (bottom * height + margin).toInt().coerceAtMost(height)
        )
    }
}

object IdRedactionSuggester {
    private val passportMrzPattern = Regex("""[A-Z0-9<]{30,}""")
    private val idNumberPattern = Regex("""\b[A-Z0-9]{6,}\b""")
    private val datePattern = Regex("""\b\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}\b""")
    private val sensitiveLabelPattern = Regex(
        pattern = """(?i)\b(passport|national id|nic|license|licence|ssn|sin|dob|date of birth|document no|id no|mrz)\b"""
    )

    fun suggest(pages: List<PageOcrResult>): IdRedactionSuggestion? {
        if (pages.isEmpty()) return null

        val labels = linkedSetOf<String>()
        val regions = mutableListOf<NormalizedRegion>()

        pages.forEach { page ->
            val width = page.sourceWidth.toFloat().coerceAtLeast(1f)
            val height = page.sourceHeight.toFloat().coerceAtLeast(1f)

            page.lines.forEach { line ->
                val normalized = normalizeLine(line, width, height)
                when {
                    passportMrzPattern.containsMatchIn(line.text) -> {
                        labels += "Machine-readable zone"
                        regions += normalized.copy(label = "Machine-readable zone")
                    }
                    sensitiveLabelPattern.containsMatchIn(line.text) -> {
                        labels += "Labeled identity field"
                        regions += normalized.copy(label = "Labeled identity field")
                    }
                    idNumberPattern.containsMatchIn(line.text) && line.text.any { it.isDigit() } -> {
                        labels += "ID number"
                        regions += normalized.copy(label = "ID number")
                    }
                    datePattern.containsMatchIn(line.text) -> {
                        labels += "Date field"
                        regions += normalized.copy(label = "Date field")
                    }
                }
            }

            if (regions.none { it.label == "Machine-readable zone" }) {
                labels += "MRZ band"
                regions += NormalizedRegion(
                    label = "MRZ band",
                    left = 0.05f,
                    top = 0.82f,
                    right = 0.95f,
                    bottom = 0.98f
                )
            }
        }

        if (regions.isEmpty()) return null
        return IdRedactionSuggestion(labels = labels.toList(), regions = regions.distinctBy { regionKey(it) })
    }

    fun suggestFromText(ocrText: String): IdRedactionSuggestion? {
        if (ocrText.isBlank()) return null
        val labels = linkedSetOf<String>()
        if (passportMrzPattern.containsMatchIn(ocrText)) labels += "Machine-readable zone"
        if (sensitiveLabelPattern.containsMatchIn(ocrText)) labels += "Labeled identity field"
        if (idNumberPattern.containsMatchIn(ocrText)) labels += "ID number"
        if (datePattern.containsMatchIn(ocrText)) labels += "Date field"
        if (labels.isEmpty()) return null

        return IdRedactionSuggestion(
            labels = labels.toList(),
            regions = listOf(
                NormalizedRegion("MRZ band", 0.05f, 0.82f, 0.95f, 0.98f),
                NormalizedRegion("ID number band", 0.08f, 0.42f, 0.92f, 0.58f)
            )
        )
    }

    private fun normalizeLine(
        line: OcrLine,
        width: Float,
        height: Float
    ): NormalizedRegion = NormalizedRegion(
        label = "Detected text",
        left = line.left / width,
        top = line.top / height,
        right = line.right / width,
        bottom = line.bottom / height
    )

    private fun regionKey(region: NormalizedRegion): String =
        "${region.label}:${region.left}:${region.top}:${region.right}:${region.bottom}"
}
