package com.ardeno.clearscan.ocr

import android.content.Context
import com.ardeno.clearscan.R

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

    fun suggest(context: Context, pages: List<PageOcrResult>): IdRedactionSuggestion? {
        if (pages.isEmpty()) return null

        val labelMrz = context.getString(R.string.id_label_mrz)
        val labelIdentity = context.getString(R.string.id_label_identity_field)
        val labelIdNumber = context.getString(R.string.id_label_id_number)
        val labelDate = context.getString(R.string.id_label_date_field)
        val labelMrzBand = context.getString(R.string.id_label_mrz_band)
        val labelDetected = context.getString(R.string.id_label_detected_text)

        val labels = linkedSetOf<String>()
        val regions = mutableListOf<NormalizedRegion>()

        pages.forEach { page ->
            val width = page.sourceWidth.toFloat().coerceAtLeast(1f)
            val height = page.sourceHeight.toFloat().coerceAtLeast(1f)

            page.lines.forEach { line ->
                val normalized = normalizeLine(line, width, height, labelDetected)
                when {
                    passportMrzPattern.containsMatchIn(line.text) -> {
                        labels += labelMrz
                        regions += normalized.copy(label = labelMrz)
                    }
                    sensitiveLabelPattern.containsMatchIn(line.text) -> {
                        labels += labelIdentity
                        regions += normalized.copy(label = labelIdentity)
                    }
                    idNumberPattern.containsMatchIn(line.text) && line.text.any { it.isDigit() } -> {
                        labels += labelIdNumber
                        regions += normalized.copy(label = labelIdNumber)
                    }
                    datePattern.containsMatchIn(line.text) -> {
                        labels += labelDate
                        regions += normalized.copy(label = labelDate)
                    }
                }
            }

            if (regions.none { it.label == labelMrz }) {
                labels += labelMrzBand
                regions += NormalizedRegion(
                    label = labelMrzBand,
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

    fun suggestFromText(context: Context, ocrText: String): IdRedactionSuggestion? {
        if (ocrText.isBlank()) return null

        val labelMrz = context.getString(R.string.id_label_mrz)
        val labelIdentity = context.getString(R.string.id_label_identity_field)
        val labelIdNumber = context.getString(R.string.id_label_id_number)
        val labelDate = context.getString(R.string.id_label_date_field)
        val labelMrzBand = context.getString(R.string.id_label_mrz_band)
        val labelIdNumberBand = context.getString(R.string.id_label_id_number_band)

        val labels = linkedSetOf<String>()
        if (passportMrzPattern.containsMatchIn(ocrText)) labels += labelMrz
        if (sensitiveLabelPattern.containsMatchIn(ocrText)) labels += labelIdentity
        if (idNumberPattern.containsMatchIn(ocrText)) labels += labelIdNumber
        if (datePattern.containsMatchIn(ocrText)) labels += labelDate
        if (labels.isEmpty()) return null

        return IdRedactionSuggestion(
            labels = labels.toList(),
            regions = listOf(
                NormalizedRegion(labelMrzBand, 0.05f, 0.82f, 0.95f, 0.98f),
                NormalizedRegion(labelIdNumberBand, 0.08f, 0.42f, 0.92f, 0.58f)
            )
        )
    }

    private fun normalizeLine(
        line: OcrLine,
        width: Float,
        height: Float,
        label: String
    ): NormalizedRegion = NormalizedRegion(
        label = label,
        left = line.left / width,
        top = line.top / height,
        right = line.right / width,
        bottom = line.bottom / height
    )

    private fun regionKey(region: NormalizedRegion): String =
        "${region.label}:${region.left}:${region.top}:${region.right}:${region.bottom}"
}
