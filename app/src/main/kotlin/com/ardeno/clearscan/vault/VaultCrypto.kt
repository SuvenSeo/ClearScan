package com.ardeno.clearscan.vault

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultCiphertext(
    val iv: ByteArray,
    val ciphertext: ByteArray
)

interface VaultCipher {
    fun ensureVaultKey()
    fun encrypt(bytes: ByteArray): VaultCiphertext
    fun decrypt(ciphertext: VaultCiphertext): ByteArray
}

class VaultCrypto : VaultCipher {
    @Volatile
    private var sessionAuthorized = false

    fun markSessionAuthorized() {
        sessionAuthorized = true
    }

    fun clearSession() {
        sessionAuthorized = false
    }

    fun requiresAuthentication(): Boolean =
        hasBiometricKey() && !sessionAuthorized

    fun hasBiometricKey(): Boolean = loadKey(KEY_ALIAS_BIOMETRIC) != null

    fun hasLegacyKey(): Boolean = loadKey(KEY_ALIAS) != null

    override fun ensureVaultKey() {
        if (loadKey(KEY_ALIAS) != null || loadKey(KEY_ALIAS_BIOMETRIC) != null) return
        createLegacyVaultKey()
    }

    fun ensureBiometricVaultKey() {
        if (loadKey(KEY_ALIAS_BIOMETRIC) != null) return
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(buildBiometricKeySpec())
        keyGenerator.generateKey()
    }

    fun createDecryptCipher(): Cipher {
        ensureBiometricVaultKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, requireNotNull(loadKey(KEY_ALIAS_BIOMETRIC)))
        return cipher
    }

    override fun encrypt(bytes: ByteArray): VaultCiphertext {
        val alias = activeEncryptionKeyAlias()
        if (alias == KEY_ALIAS_BIOMETRIC && !sessionAuthorized) {
            throw VaultAuthenticationRequiredException()
        }
        ensureKeyForAlias(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, requireNotNull(loadKey(alias)))
        return VaultCiphertext(
            iv = cipher.iv,
            ciphertext = cipher.doFinal(bytes)
        )
    }

    override fun decrypt(ciphertext: VaultCiphertext): ByteArray {
        val alias = activeDecryptionKeyAlias()
        if (alias == KEY_ALIAS_BIOMETRIC && !sessionAuthorized) {
            throw VaultAuthenticationRequiredException()
        }
        return decryptWithAlias(alias, ciphertext)
    }

    fun decryptWithLegacyKey(ciphertext: VaultCiphertext): ByteArray {
        require(hasLegacyKey()) { "Legacy vault key is not available." }
        return decryptWithAlias(KEY_ALIAS, ciphertext)
    }

    fun healthCheck(): Boolean {
        if (requiresAuthentication()) return hasBiometricKey() || hasLegacyKey()
        val sample = "ClearScan vault".encodeToByteArray()
        val encrypted = encrypt(sample)
        return decrypt(encrypted).contentEquals(sample)
    }

    private fun activeEncryptionKeyAlias(): String =
        when {
            hasBiometricKey() -> KEY_ALIAS_BIOMETRIC
            hasLegacyKey() -> KEY_ALIAS
            else -> KEY_ALIAS
        }

    private fun activeDecryptionKeyAlias(): String =
        when {
            hasBiometricKey() -> KEY_ALIAS_BIOMETRIC
            hasLegacyKey() -> KEY_ALIAS
            else -> KEY_ALIAS
        }

    private fun ensureKeyForAlias(alias: String) {
        when (alias) {
            KEY_ALIAS_BIOMETRIC -> ensureBiometricVaultKey()
            KEY_ALIAS -> ensureVaultKey()
            else -> error("Unknown vault key alias: $alias")
        }
    }

    private fun decryptWithAlias(alias: String, ciphertext: VaultCiphertext): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            requireNotNull(loadKey(alias)),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, ciphertext.iv)
        )
        return cipher.doFinal(ciphertext.ciphertext)
    }

    private fun createLegacyVaultKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(buildLegacyKeySpec())
        keyGenerator.generateKey()
    }

    private fun buildLegacyKeySpec(): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

    private fun buildBiometricKeySpec(): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS_BIOMETRIC,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        }

        return builder.build()
    }

    private fun loadKey(alias: String): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "clearscan_vault_aes"
        const val KEY_ALIAS_BIOMETRIC = "clearscan_vault_aes_bio"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
