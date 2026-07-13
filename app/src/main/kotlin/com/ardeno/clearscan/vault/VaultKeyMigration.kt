package com.ardeno.clearscan.vault

import android.content.Context
import java.io.File
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
            .forEach { encryptedFile -> reEncryptFile(encryptedFile) }

        vaultSettings.setKeyVersion(VaultSettings.KEY_VERSION_BIOMETRIC)
        vaultSettings.setAuthMode(VaultSettings.AUTH_MODE_BIOMETRIC)
    }

    private fun reEncryptFile(encryptedFile: File) {
        val ciphertext = EncryptedFileStore.unpackCiphertext(encryptedFile.readBytes())
        val plaintext = vaultCrypto.decryptWithLegacyKey(ciphertext)
        val payload = vaultCrypto.encrypt(plaintext)
        encryptedFile.outputStream().use { output ->
            output.write(EncryptedFileStore.packCiphertext(payload))
        }
    }
}
