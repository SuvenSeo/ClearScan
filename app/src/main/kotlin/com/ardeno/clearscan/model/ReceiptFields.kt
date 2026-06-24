package com.ardeno.clearscan.model

data class ReceiptFields(
    val merchant: String? = null,
    val amount: String? = null,
    val date: String? = null
) {
    val hasAnyField: Boolean
        get() = merchant != null || amount != null || date != null
}
