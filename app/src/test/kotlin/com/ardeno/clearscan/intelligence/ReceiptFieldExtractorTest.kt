package com.ardeno.clearscan.intelligence

import com.ardeno.clearscan.testing.RobolectricUnitTest

import com.ardeno.clearscan.model.ReceiptFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFieldExtractorTest : RobolectricUnitTest() {
    @Test
    fun extractsReceiptFieldsFromTypicalText() {
        val text = """
            COLOMBO MART (PVT) LTD
            123 Galle Road
            Date: 12/06/2026
            Item A          450.00
            TOTAL           LKR 1,250.50
            Thank you
        """.trimIndent()

        val fields = ReceiptFieldExtractor.extract(text)

        assertNotNull(fields.merchant)
        assertTrue(fields.merchant!!.contains("COLOMBO", ignoreCase = true))
        assertEquals("1250.50", fields.amount)
        assertEquals("12/06/2026", fields.date)
    }

    @Test
    fun returnsEmptyWhenNoSignals() {
        val fields = ReceiptFieldExtractor.extract("")

        assertFalse(fields.hasAnyField)
    }
}
