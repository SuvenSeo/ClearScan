package com.ardeno.clearscan.export

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaperlessExporter {
    suspend fun uploadDocument(
        baseUrl: String,
        apiToken: String,
        file: File,
        title: String
    ): String = withContext(Dispatchers.IO) {
        require(file.exists()) { "Export file is missing." }
        val endpoint = buildEndpoint(baseUrl)
        val boundary = "ClearScan-${UUID.randomUUID()}"
        val connection = URI(endpoint).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Token $apiToken")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        connection.outputStream.buffered().use { output ->
            writeField(output, boundary, "title", title)
            writeFileField(output, boundary, "document", file)
            output.write("--$boundary--\r\n".toByteArray())
        }

        when (val code = connection.responseCode) {
            in 200..299 -> {
                val body = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                "paperless:$endpoint:$body"
            }
            else -> {
                val message = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                connection.disconnect()
                error(
                    buildString {
                        append("Paperless upload failed ($code).")
                        if (message.isNotBlank()) append(' ').append(message.trim())
                    }
                )
            }
        }
    }

    private fun buildEndpoint(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/api/documents/post_document")) {
            normalized
        } else {
            "$normalized/api/documents/post_document/"
        }
    }

    private fun writeField(
        output: java.io.OutputStream,
        boundary: String,
        name: String,
        value: String
    ) {
        output.write("--$boundary\r\n".toByteArray())
        output.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
        output.write(value.toByteArray())
        output.write("\r\n".toByteArray())
    }

    private fun writeFileField(
        output: java.io.OutputStream,
        boundary: String,
        name: String,
        file: File
    ) {
        output.write("--$boundary\r\n".toByteArray())
        output.write(
            "Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n".toByteArray()
        )
        output.write("Content-Type: application/pdf\r\n\r\n".toByteArray())
        file.inputStream().use { input -> input.copyTo(output) }
        output.write("\r\n".toByteArray())
    }
}
