package com.ardeno.clearscan.intelligence

object DocumentTagger {
    private val receiptSignals = listOf(
        "receipt", "thank you for your purchase", "cashier", "change due", "subtotal", "qty", "item"
    )

    private val invoiceSignals = listOf(
        "invoice", "bill to", "ship to", "payment terms", "due date", "tax id", "vat number", "po number"
    )

    private val certificateSignals = listOf(
        "certificate", "certifies", "awarded to", "this is to certify", "diploma", "degree", "completion"
    )

    private val idSignals = listOf(
        "passport", "driver", "licence", "license", "national id", "identity card", "date of birth", "nid"
    )

    fun suggestTags(ocrText: String): List<String> {
        if (ocrText.isBlank()) return emptyList()

        val normalized = ocrText.lowercase()
        val tags = buildList {
            if (receiptSignals.any { normalized.contains(it) }) add("receipt")
            if (invoiceSignals.any { normalized.contains(it) }) add("invoice")
            if (certificateSignals.any { normalized.contains(it) }) add("certificate")
            if (idSignals.any { normalized.contains(it) }) add("id")
        }

        return tags.distinct()
    }
}
