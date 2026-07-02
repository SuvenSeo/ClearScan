package com.ardeno.clearscan.export

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.SelfHostTargetType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelfHostExporter(
    private val context: Context? = null,
    private val webDavClient: WebDavClient = WebDavClient(),
    private val paperlessExporter: PaperlessExporter = PaperlessExporter()
) {
    suspend fun export(
        document: ScanDocument,
        exportFile: File,
        config: SelfHostConfig,
        wifiOnly: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        require(config.enabled) { "Self-host export is disabled." }
        require(config.isConfigured) { "Configure your self-host endpoint in Settings first." }

        if (wifiOnly) {
            checkWifiOrThrow()
        }

        when (config.targetType) {
            SelfHostTargetType.WebDav -> webDavClient.upload(
                baseUrl = config.baseUrl,
                remoteFolder = config.remoteFolder,
                fileName = exportFile.name,
                file = exportFile,
                username = config.username,
                password = config.password
            )
            SelfHostTargetType.PaperlessNgx -> paperlessExporter.uploadDocument(
                baseUrl = config.baseUrl,
                apiToken = config.apiToken,
                file = exportFile,
                title = document.title
            )
        }
    }

    private fun checkWifiOrThrow() {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        val network = cm.activeNetwork ?: throwWifiError()
        val caps = cm.getNetworkCapabilities(network) ?: throwWifiError()
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            throwWifiError()
        }
    }

    private fun throwWifiError(): Nothing {
        error("Wi-Fi only upload is enabled. Connect to a Wi-Fi network first.")
    }

    fun resolveExportFile(document: ScanDocument): File? =
        listOfNotNull(document.searchablePdfPath, document.pdfPath)
            .map(::File)
            .firstOrNull { it.exists() }
            ?: document.pageImagePaths
                .map(::File)
                .firstOrNull { it.exists() }
}
