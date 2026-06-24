package com.ardeno.clearscan.vault

import android.content.Context
import java.io.File
import java.nio.ByteBuffer

class EncryptedFileStore(
    private val context: Context,
    private val vaultCrypto: VaultCrypto
) {
    fun storageRoot(): File = File(context.filesDir, "documents").apply { mkdirs() }

    fun readableCacheRoot(documentId: String): File =
        File(context.cacheDir, "vault-read/$documentId").apply { mkdirs() }

    fun isEncryptedPath(path: String): Boolean = path.endsWith(ENCRYPTED_SUFFIX)

    fun encryptedSibling(plaintext: File): File =
        File(plaintext.parentFile, "${plaintext.name}$ENCRYPTED_SUFFIX")

    fun plaintextName(encrypted: File): String =
        encrypted.name.removeSuffix(ENCRYPTED_SUFFIX)

    fun encryptPlaintextFile(source: File): File {
        require(source.exists()) { "Missing file: ${source.absolutePath}" }
        vaultCrypto.ensureVaultKey()

        val target = encryptedSibling(source)
        val payload = vaultCrypto.encrypt(source.readBytes())
        target.outputStream().use { output ->
            output.write(MAGIC)
            output.write(payload.iv)
            output.write(payload.ciphertext)
        }
        source.delete()
        return target
    }

    fun migratePlaintextIfNeeded(source: File): File {
        if (!source.exists()) return encryptedSibling(source).takeIf { it.exists() } ?: source
        if (isEncryptedPath(source.name)) return source
        return encryptPlaintextFile(source)
    }

    fun decryptToCache(encryptedPath: String, documentId: String): File {
        val encrypted = File(encryptedPath)
        require(encrypted.exists()) { "Encrypted file is missing: $encryptedPath" }

        val cacheDir = readableCacheRoot(documentId)
        val cacheFile = File(cacheDir, plaintextName(encrypted))
        if (cacheFile.exists() && cacheFile.lastModified() >= encrypted.lastModified()) {
            return cacheFile
        }

        val plaintext = decryptFile(encrypted)
        cacheFile.outputStream().use { output ->
            output.write(plaintext)
        }
        return cacheFile
    }

    fun writeEncryptedBytes(parentDir: File, fileName: String, bytes: ByteArray): File {
        vaultCrypto.ensureVaultKey()
        val target = File(parentDir, "$fileName$ENCRYPTED_SUFFIX")
        val payload = vaultCrypto.encrypt(bytes)
        target.outputStream().use { output ->
            output.write(MAGIC)
            output.write(payload.iv)
            output.write(payload.ciphertext)
        }
        return target
    }

    fun readEncryptedBytes(encrypted: File): ByteArray = decryptFile(encrypted)

    fun clearReadableCache(documentId: String) {
        File(context.cacheDir, "vault-read/$documentId").deleteRecursively()
    }

    fun clearAllReadableCache() {
        File(context.cacheDir, "vault-read").deleteRecursively()
    }

    private fun decryptFile(encrypted: File): ByteArray {
        val bytes = encrypted.readBytes()
        require(bytes.size > MAGIC.size + GCM_IV_LENGTH) { "Encrypted file is too small." }
        val magic = bytes.copyOfRange(0, MAGIC.size)
        require(magic.contentEquals(MAGIC)) { "Unrecognized encrypted file header." }

        val iv = bytes.copyOfRange(MAGIC.size, MAGIC.size + GCM_IV_LENGTH)
        val ciphertext = bytes.copyOfRange(MAGIC.size + GCM_IV_LENGTH, bytes.size)
        return vaultCrypto.decrypt(VaultCiphertext(iv = iv, ciphertext = ciphertext))
    }

    companion object {
        private val MAGIC = "CSC1".encodeToByteArray()
        private const val GCM_IV_LENGTH = 12
        const val ENCRYPTED_SUFFIX = ".enc"

        fun packCiphertext(ciphertext: VaultCiphertext): ByteArray =
            ByteBuffer.allocate(MAGIC.size + ciphertext.iv.size + ciphertext.ciphertext.size)
                .put(MAGIC)
                .put(ciphertext.iv)
                .put(ciphertext.ciphertext)
                .array()

        fun unpackCiphertext(bytes: ByteArray): VaultCiphertext {
            require(bytes.size > MAGIC.size + GCM_IV_LENGTH) { "Backup payload is too small." }
            require(bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "Invalid backup header." }
            val iv = bytes.copyOfRange(MAGIC.size, MAGIC.size + GCM_IV_LENGTH)
            val payload = bytes.copyOfRange(MAGIC.size + GCM_IV_LENGTH, bytes.size)
            return VaultCiphertext(iv = iv, ciphertext = payload)
        }
    }
}
