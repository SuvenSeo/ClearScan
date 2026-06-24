package com.ardeno.clearscan.export

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDavClient {
    suspend fun upload(
        baseUrl: String,
        remoteFolder: String,
        fileName: String,
        file: File,
        username: String,
        password: String
    ): String = withContext(Dispatchers.IO) {
        require(file.exists()) { "Export file is missing." }
        val targetUrl = buildTargetUrl(baseUrl, remoteFolder, fileName)
        ensureCollection(targetUrl.parentPath(), username, password)
        putFile(targetUrl, file, username, password)
        targetUrl
    }

    private fun buildTargetUrl(
        baseUrl: String,
        remoteFolder: String,
        fileName: String
    ): String {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        val segments = buildList {
            if (remoteFolder.isNotBlank()) {
                addAll(remoteFolder.split('/').filter { it.isNotBlank() })
            }
            add(fileName)
        }
        val encodedPath = segments.joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
        return "$normalizedBase/$encodedPath"
    }

    private suspend fun ensureCollection(
        collectionUrl: String,
        username: String,
        password: String
    ) {
        val connection = openConnection(collectionUrl, "MKCOL", username, password)
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_CREATED,
                HttpURLConnection.HTTP_OK,
                HttpURLConnection.HTTP_NO_CONTENT,
                405,
                409 -> Unit
                else -> {
                    val message = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                    if (message.isNotBlank()) {
                        error("WebDAV folder setup failed (${connection.responseCode}): $message")
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun putFile(
        targetUrl: String,
        file: File,
        username: String,
        password: String
    ) {
        val connection = openConnection(targetUrl, "PUT", username, password)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(file.length())
            file.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            when (val code = connection.responseCode) {
                in 200..299 -> Unit
                else -> {
                    val message = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                    error(
                        buildString {
                            append("WebDAV upload failed ($code).")
                            if (message.isNotBlank()) append(' ').append(message.trim())
                        }
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        url: String,
        method: String,
        username: String,
        password: String
    ): HttpURLConnection {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty(
            "Authorization",
            "Basic ${encodeBasicCredentials(username, password)}"
        )
        return connection
    }

    private fun encodeBasicCredentials(username: String, password: String): String {
        val raw = "$username:$password"
        return Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
    }

    private fun String.parentPath(): String {
        val lastSlash = lastIndexOf('/')
        return if (lastSlash <= 0) this else substring(0, lastSlash)
    }
}
