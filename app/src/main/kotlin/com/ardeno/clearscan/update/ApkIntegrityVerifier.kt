package com.ardeno.clearscan.update

import java.io.File
import java.security.MessageDigest

object ApkIntegrityVerifier {
    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun verify(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) {
            return true
        }
        return sha256Of(file).equals(expectedSha256, ignoreCase = true)
    }
}
