package com.ardeno.clearscan.vault

import android.content.Context
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultKeyMigration(
    context: Context,
    private val vaultCrypto: VaultCrypto,
    private val encryptedFileStore: EncryptedFileStore,
    private val vaultSettings: VaultSettings
) {
    private val documentsRoot = File(context.filesDir, "documents")

    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        if (vaultSettings.getKeyVersion() >= VaultSettings.KEY_VERSION_BIOMETRIC) return@withContext
        if (!vaultCrypto.hasLegacyKey()) {
            vaultSettings.setKeyVersion(VaultSettings.KEY_VERSION_BIOMETRIC)
            vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_BIOMETRIC)
            return@withContext
        }
        if (vaultCrypto.requiresAuthentication()) return@withContext

        vaultCrypto.ensureBiometricVaultKey()

        documentsRoot.walkTopDown()
            .filter { it.isFile && encryptedFileStore.isEncryptedPath(it.absolutePath) }
            .forEach { encryptedFile -> reEncryptLegacyToBiometric(encryptedFile) }

        vaultSettings.setKeyVersion(VaultSettings.KEY_VERSION_BIOMETRIC)
        vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_BIOMETRIC)
    }

    /** Re-encrypt all blobs with the legacy key and delete the biometric Keystore alias. */
    suspend fun downgradeToLegacyIfNeeded() = withContext(Dispatchers.IO) {
        if (!vaultCrypto.hasBiometricKey()) {
            vaultSettings.setKeyVersion(VaultSettings.KEY_VERSION_LEGACY)
            vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_NONE)
            return@withContext
        }
        if (vaultCrypto.requiresAuthentication()) {
            throw VaultAuthenticationRequiredException()
        }
        vaultCrypto.ensureVaultKey()

        vaultCrypto.disableBiometricKey { crypto ->
            documentsRoot.walkTopDown()
                .filter { it.isFile && encryptedFileStore.isEncryptedPath(it.absolutePath) }
                .forEach { encryptedFile -> reEncryptBiometricToLegacy(encryptedFile, crypto) }
        }

        vaultSettings.setKeyVersion(VaultSettings.KEY_VERSION_LEGACY)
        vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_NONE)
    }

    private fun reEncryptLegacyToBiometric(encryptedFile: File) {
        val ciphertext = EncryptedFileStore.unpackCiphertext(encryptedFile.readBytes())
        val plaintext = vaultCrypto.decryptWithLegacyKey(ciphertext)
        val payload = vaultCrypto.encrypt(plaintext)
        encryptedFile.outputStream().use { output ->
            output.write(EncryptedFileStore.packCiphertext(payload))
        }
    }

    private fun reEncryptBiometricToLegacy(encryptedFile: File, crypto: VaultCrypto) {
        val ciphertext = EncryptedFileStore.unpackCiphertext(encryptedFile.readBytes())
        val plaintext = crypto.decryptWithBiometricKey(ciphertext)
        val legacyPayload = encryptWithLegacyKey(plaintext)
        encryptedFile.outputStream().use { output ->
            output.write(EncryptedFileStore.packCiphertext(legacyPayload))
        }
    }

    private fun encryptWithLegacyKey(plaintext: ByteArray): VaultCiphertext {
        val keyStore = KeyStore.getInstance(VaultCrypto.ANDROID_KEYSTORE).apply { load(null) }
        val legacyKey = keyStore.getKey(VaultCrypto.KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(VaultCrypto.TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, legacyKey)
        return VaultCiphertext(iv = cipher.iv, ciphertext = cipher.doFinal(plaintext))
    }
}
