package com.ardeno.clearscan.intelligence

import com.ardeno.clearscan.testing.RobolectricUnitTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTaggerTest : RobolectricUnitTest() {
    @Test
    fun suggestsReceiptTag() {
        val tags = DocumentTagger.suggestTags("Store receipt\nSubtotal 12.00\nThank you")

        assertTrue(tags.contains("receipt"))
    }

    @Test
    fun suggestsMultipleTagsForInvoiceLikeText() {
        val tags = DocumentTagger.suggestTags("INVOICE\nBill To: Acme Corp\nPayment Terms: Net 30")

        assertEquals(listOf("invoice"), tags)
    }

    @Test
    fun returnsEmptyForBlankText() {
        assertTrue(DocumentTagger.suggestTags("   ").isEmpty())
    }
}
