package com.ardeno.clearscan.export

import com.ardeno.clearscan.data.SelfHostConfig
import com.ardeno.clearscan.model.ScanDocument
import com.ardeno.clearscan.model.SelfHostTargetType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelfHostExporter(
    private val webDavClient: WebDavClient = WebDavClient(),
    private val paperlessExporter: PaperlessExporter = PaperlessExporter()
) {
    suspend fun export(
        document: ScanDocument,
        exportFile: File,
        config: SelfHostConfig
    ): String = withContext(Dispatchers.IO) {
        require(config.enabled) { "Self-host export is disabled." }
        require(config.isConfigured) { "Configure your self-host endpoint in Settings first." }

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

    fun resolveExportFile(document: ScanDocument): File? =
        listOfNotNull(document.searchablePdfPath, document.pdfPath)
            .map(::File)
            .firstOrNull { it.exists() }
            ?: document.pageImagePaths
                .map(::File)
                .firstOrNull { it.exists() }
}
