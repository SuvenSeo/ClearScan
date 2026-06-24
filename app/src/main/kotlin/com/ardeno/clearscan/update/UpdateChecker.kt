package com.ardeno.clearscan.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateChecker(
    private val manifestUrl: String
) {
    suspend fun fetchLatest(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    error("Update server returned HTTP $code.")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                AppUpdateManifestParser.parse(body)
            } finally {
                connection.disconnect()
            }
        }
    }
}
