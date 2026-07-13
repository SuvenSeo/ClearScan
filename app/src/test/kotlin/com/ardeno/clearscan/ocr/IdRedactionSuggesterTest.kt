package com.ardeno.clearscan.ocr

import com.ardeno.clearscan.testing.RobolectricUnitTest

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdRedactionSuggesterTest : RobolectricUnitTest() {
    @Test
    fun suggestFromText_detectsPassportMrz() {
        val suggestion = IdRedactionSuggester.suggestFromText(
            "P<USASMITH<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<"
        )
        assertNotNull(suggestion)
        assertTrue(suggestion!!.labels.any { it.contains("Machine-readable", ignoreCase = true) })
    }

    @Test
    fun suggestFromText_detectsIdNumber() {
        val suggestion = IdRedactionSuggester.suggestFromText("Document no: AB123456789")
        assertNotNull(suggestion)
        assertTrue(suggestion!!.labels.any { it.contains("ID", ignoreCase = true) })
    }
}
