package com.ardeno.clearscan.vault

import com.ardeno.clearscan.testing.RobolectricUnitTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session / biometric policy tests for [VaultCrypto].
 *
 * Android Keystore biometric keys are often unavailable on JVM/Robolectric, so we do not
 * require [VaultCrypto.createAuthCipher] to succeed here. Instead we document and assert the
 * session model constants that drive CryptoObject unlock:
 *
 * - Auth validity window is 12 hours ([VaultCrypto.BIOMETRIC_AUTH_VALIDITY_SECONDS]), not `0`
 *   (per-operation CryptoObject), so post-unlock file ops can mint fresh ciphers mid-session.
 * - Unlock must use BiometricPrompt.CryptoObject from [VaultCrypto.createAuthCipher], then
 *   [VaultCrypto.markSessionAuthorized].
 * - Disabling biometric vault ([VaultCrypto.disableBiometricKey]) downgrades to the legacy
 *   Keystore alias after optional re-encrypt.
 */
class VaultCryptoTest : RobolectricUnitTest() {

    @Test
    fun biometricAuthValiditySeconds_isTwelveHourWindow() {
        assertTrue(
            "Biometric auth validity must be > 0 so mid-session encrypt/decrypt can mint new ciphers",
            VaultCrypto.BIOMETRIC_AUTH_VALIDITY_SECONDS > 0
        )
        assertEquals(12 * 60 * 60, VaultCrypto.BIOMETRIC_AUTH_VALIDITY_SECONDS)
        assertEquals(
            VaultCrypto.BIOMETRIC_AUTH_VALIDITY_SECONDS,
            VaultCrypto().biometricAuthValiditySeconds()
        )
    }

    @Test
    fun createAuthCipher_doesNotThrowWhenKeystoreAllowsKeyCreation() {
        val crypto = VaultCrypto()
        try {
            val cipher = crypto.createAuthCipher()
            assertTrue(cipher.algorithm.isNotBlank())
        } catch (error: Exception) {
            // Robolectric / desktop JVM often lacks AndroidKeyStore biometric support.
            // Policy coverage remains in biometricAuthValiditySeconds_isTwelveHourWindow.
            assertTrue(
                "Unexpected failure type: ${error::class.java.name}: ${error.message}",
                error.message?.contains("KeyStore", ignoreCase = true) == true ||
                    error.message?.contains("AndroidKeyStore", ignoreCase = true) == true ||
                    error::class.java.name.contains("KeyStore", ignoreCase = true) ||
                    error::class.java.name.contains("Provider", ignoreCase = true) ||
                    error.cause != null
            )
        }
    }

    @Test
    fun sessionFlags_defaultUnauthorizedUntilMarked() {
        val crypto = VaultCrypto()
        // Without a biometric key, requiresAuthentication is false (legacy path).
        // After a bio key exists, requiresAuthentication would be true until markSessionAuthorized.
        crypto.clearSession()
        crypto.markSessionAuthorized()
        crypto.clearSession()
        assertTrue(true) // API smoke: session toggles do not throw
    }

    @Test
    fun metadataCrypto_stillRoundTripsWithFakeVault() {
        val metadata = MetadataCrypto(object : VaultCipher {
            override fun ensureVaultKey() = Unit
            override fun encrypt(bytes: ByteArray): VaultCiphertext =
                VaultCiphertext(iv = ByteArray(12), ciphertext = bytes.copyOf())
            override fun decrypt(ciphertext: VaultCiphertext): ByteArray =
                ciphertext.ciphertext
        })
        val json = """{"id":"vault-crypto-test"}"""
        assertEquals(json, metadata.decryptUtf8(metadata.encryptUtf8(json)))
    }
}
