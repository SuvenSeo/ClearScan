package com.ardeno.clearscan.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

data class VaultCiphertext(
    val iv: ByteArray,
    val ciphertext: ByteArray
)

class VaultCrypto {
    fun ensureVaultKey() {
        if (loadKey() != null) return

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    fun encrypt(bytes: ByteArray): VaultCiphertext {
        ensureVaultKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, requireNotNull(loadKey()))
        return VaultCiphertext(
            iv = cipher.iv,
            ciphertext = cipher.doFinal(bytes)
        )
    }

    fun decrypt(ciphertext: VaultCiphertext): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            requireNotNull(loadKey()),
            javax.crypto.spec.GCMParameterSpec(128, ciphertext.iv)
        )
        return cipher.doFinal(ciphertext.ciphertext)
    }

    fun healthCheck(): Boolean {
        val sample = "ClearScan vault".encodeToByteArray()
        val encrypted = encrypt(sample)
        return decrypt(encrypted).contentEquals(sample)
    }

    private fun loadKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "clearscan_vault_aes"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
