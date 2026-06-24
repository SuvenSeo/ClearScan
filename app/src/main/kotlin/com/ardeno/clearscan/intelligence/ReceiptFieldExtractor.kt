package com.ardeno.clearscan.intelligence

import com.ardeno.clearscan.model.ReceiptFields

object ReceiptFieldExtractor {
    private val datePatterns = listOf(
        Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b"""),
        Regex("""\b(\d{4}-\d{2}-\d{2})\b"""),
        Regex(
            """\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4})\b""",
            RegexOption.IGNORE_CASE
        )
    )

    private val amountPatterns = listOf(
        Regex(
            """(?:total|amount|balance|due|subtotal|grand\s+total)[:\s]*(?:Rs\.?|LKR|USD|\$|£|€|₹)?\s*([\d,]+\.\d{2})""",
            RegexOption.IGNORE_CASE
        ),
        Regex("""(?:Rs\.?|LKR|USD|\$|£|€|₹)\s*([\d,]+\.\d{2})"""),
        Regex("""\b([\d,]+\.\d{2})\b""")
    )

    private val merchantSkipWords = setOf(
        "receipt", "invoice", "thank", "welcome", "store", "shop", "tel", "phone", "date", "time", "total"
    )

    fun extract(ocrText: String): ReceiptFields {
        val lines = ocrText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return ReceiptFields(
            merchant = extractMerchant(lines),
            amount = extractAmount(ocrText),
            date = extractDate(ocrText)
        )
    }

    private fun extractDate(text: String): String? {
        for (pattern in datePatterns) {
            val match = pattern.find(text) ?: continue
            return match.groupValues[1].trim()
        }
        return null
    }

    private fun extractAmount(text: String): String? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text) ?: continue
            return match.groupValues[1].replace(",", "")
        }
        return null
    }

    private fun extractMerchant(lines: List<String>): String? {
        return lines
            .take(6)
            .map { line -> line.replace(Regex("""[^\w\s&'.-]"""), " ").trim() }
            .firstOrNull { line ->
                line.length in 3..48 &&
                    line.any { it.isLetter() } &&
                    merchantSkipWords.none { skip -> line.equals(skip, ignoreCase = true) }
            }
    }
}
