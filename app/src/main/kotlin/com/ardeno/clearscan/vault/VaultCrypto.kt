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

/**
 * AES/GCM vault crypto backed by Android Keystore.
 *
 * Biometric-bound keys use a non-zero authentication validity window so that a successful
 * [BiometricPrompt.CryptoObject] unlock (via [createAuthCipher] + [markSessionAuthorized])
 * authorizes subsequent encrypt/decrypt operations for the remainder of the vault session.
 * Timeout `0` (per-op CryptoObject) is intentionally avoided: the app re-creates ciphers for
 * each file operation after unlock.
 */
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

    /** Cipher presented to BiometricPrompt as CryptoObject to satisfy Keystore user-auth. */
    fun createAuthCipher(): Cipher {
        ensureBiometricVaultKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, requireNotNull(loadKey(KEY_ALIAS_BIOMETRIC)))
        return cipher
    }

    @Deprecated("Use createAuthCipher()", ReplaceWith("createAuthCipher()"))
    fun createDecryptCipher(): Cipher = createAuthCipher()

    /**
     * Disables biometric-bound encryption: ensures a legacy key, optionally re-encrypts
     * [reEncrypt] payloads with the legacy key, then deletes the biometric Keystore alias.
     */
    fun disableBiometricKey(reEncrypt: ((VaultCrypto) -> Unit)? = null) {
        ensureVaultKey()
        if (!hasBiometricKey()) {
            clearSession()
            return
        }
        // Allow decrypt with bio key while session still authorized (caller should have unlocked).
        if (requiresAuthentication()) {
            throw VaultAuthenticationRequiredException()
        }
        reEncrypt?.invoke(this)
        deleteKey(KEY_ALIAS_BIOMETRIC)
        clearSession()
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

    fun decryptWithBiometricKey(ciphertext: VaultCiphertext): ByteArray {
        require(hasBiometricKey()) { "Biometric vault key is not available." }
        if (!sessionAuthorized) throw VaultAuthenticationRequiredException()
        return decryptWithAlias(KEY_ALIAS_BIOMETRIC, ciphertext)
    }

    fun healthCheck(): Boolean {
        if (requiresAuthentication()) return hasBiometricKey() || hasLegacyKey()
        val sample = "ClearScan vault".encodeToByteArray()
        val encrypted = encrypt(sample)
        return decrypt(encrypted).contentEquals(sample)
    }

    fun biometricAuthValiditySeconds(): Int = BIOMETRIC_AUTH_VALIDITY_SECONDS

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
            KEY_ALIAS -> {
                if (loadKey(KEY_ALIAS) == null) createLegacyVaultKey()
            }
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
        if (loadKey(KEY_ALIAS) != null) return
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
            // Non-zero validity window: after BiometricPrompt CryptoObject auth, file crypto
            // can create fresh Cipher instances without per-op prompts during the vault session.
            builder.setUserAuthenticationParameters(
                BIOMETRIC_AUTH_VALIDITY_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(BIOMETRIC_AUTH_VALIDITY_SECONDS)
        }

        return builder.build()
    }

    private fun loadKey(alias: String): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
    }

    private fun deleteKey(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "clearscan_vault_aes"
        const val KEY_ALIAS_BIOMETRIC = "clearscan_vault_aes_bio"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        /** Matches typical vault session: re-auth after this many seconds without re-prompt mid-session ops. */
        const val BIOMETRIC_AUTH_VALIDITY_SECONDS = 12 * 60 * 60
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
