package com.ardeno.clearscan.export

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import com.ardeno.clearscan.model.ScanDocument
import java.io.File

object DocumentPrintHelper {
    fun printDocument(context: Context, document: ScanDocument): Boolean {
        val pdfPath = document.searchablePdfPath ?: document.pdfPath ?: return false
        val file = File(pdfPath)
        if (!file.exists()) return false

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${document.title} — ClearScan"
        val adapter = PdfPrintDocumentAdapter(context, file, jobName)
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
        return true
    }
}
