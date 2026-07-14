package com.ardeno.clearscan.ocr

import com.ardeno.clearscan.R
import com.ardeno.clearscan.testing.RobolectricUnitTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class IdRedactionSuggesterTest : RobolectricUnitTest() {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun suggestFromText_detectsPassportMrz() {
        val suggestion = IdRedactionSuggester.suggestFromText(
            context,
            "P<USASMITH<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<"
        )
        assertNotNull(suggestion)
        val mrzLabel = context.getString(R.string.id_label_mrz)
        assertTrue(suggestion!!.labels.any { it.equals(mrzLabel, ignoreCase = true) })
    }

    @Test
    fun suggestFromText_detectsIdNumber() {
        val suggestion = IdRedactionSuggester.suggestFromText(
            context,
            "Document no: AB123456789"
        )
        assertNotNull(suggestion)
        val identityLabel = context.getString(R.string.id_label_identity_field)
        val idNumberLabel = context.getString(R.string.id_label_id_number)
        assertTrue(
            suggestion!!.labels.any {
                it.equals(identityLabel, ignoreCase = true) || it.equals(idNumberLabel, ignoreCase = true)
            }
        )
    }
}
