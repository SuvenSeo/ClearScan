package com.ardeno.clearscan.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class PassphraseBackupCrypto {
  fun encrypt(plainBytes: ByteArray, passphrase: CharArray): ByteArray {
    val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
    val key = deriveKey(passphrase, salt)
    val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    val ciphertext = cipher.doFinal(plainBytes)
    return salt + iv + ciphertext
  }

  fun decrypt(payload: ByteArray, passphrase: CharArray): ByteArray {
    require(payload.size > SALT_BYTES + IV_BYTES) { "Invalid passphrase backup payload." }
    val salt = payload.copyOfRange(0, SALT_BYTES)
    val iv = payload.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
    val ciphertext = payload.copyOfRange(SALT_BYTES + IV_BYTES, payload.size)
    val key = deriveKey(passphrase, salt)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(ciphertext)
  }

  private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
    val secret = factory.generateSecret(spec).encoded
    return SecretKeySpec(secret, "AES")
  }

  private companion object {
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val SALT_BYTES = 16
    const val IV_BYTES = 12
    const val KEY_BITS = 256
    const val GCM_TAG_BITS = 128
    const val ITERATIONS = 100_000
  }
}
