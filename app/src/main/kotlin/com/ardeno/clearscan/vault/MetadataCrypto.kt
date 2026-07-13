package com.ardeno.clearscan.vault

import java.io.File
import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject

class MetadataCrypto(
    private val vaultCipher: VaultCipher
) {
    fun ensureVaultKey() {
        vaultCipher.ensureVaultKey()
    }

    fun encryptUtf8(plaintext: String): ByteArray {
        ensureVaultKey()
        val payload = vaultCipher.encrypt(plaintext.encodeToByteArray())
        return EncryptedFileStore.packCiphertext(payload)
    }

    fun decryptUtf8(envelope: ByteArray): String {
        val payload = EncryptedFileStore.unpackCiphertext(envelope)
        return vaultCipher.decrypt(payload).decodeToString()
    }

    fun encryptPayload(plaintextJson: String): String =
        Base64.getEncoder().encodeToString(encryptUtf8(plaintextJson))

    fun decryptPayload(stored: String): String {
        if (isPlaintextJson(stored)) return stored
        val envelope = Base64.getDecoder().decode(stored)
        return decryptUtf8(envelope)
    }

    fun writeJsonFile(file: File, json: String) {
        file.parentFile?.mkdirs()
        file.writeBytes(encryptUtf8(json))
    }

    fun readJsonFile(file: File): String? {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return null
        return if (isEncryptedEnvelope(bytes)) {
            decryptUtf8(bytes)
        } else {
            val text = bytes.decodeToString()
            require(isPlaintextJson(text)) { "Unrecognized metadata file format: ${file.name}" }
            text
        }
    }

    companion object {
        private val MAGIC = "CSC1".encodeToByteArray()

        fun isEncryptedEnvelope(bytes: ByteArray): Boolean =
            bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

        fun isPlaintextJson(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return false
            val first = trimmed.first()
            if (first != '[' && first != '{') return false
            return runCatching {
                when (first) {
                    '[' -> JSONArray(trimmed)
                    '{' -> JSONObject(trimmed)
                    else -> return false
                }
                true
            }.getOrDefault(false)
        }
    }
}
