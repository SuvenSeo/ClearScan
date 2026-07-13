package com.ardeno.clearscan.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PassphraseBackupCryptoTest {
    private val crypto = PassphraseBackupCrypto()

    @Test
    fun encryptDecryptRoundTrip() {
        val plainBytes = "ClearScan vault export payload".toByteArray(Charsets.UTF_8)
        val passphrase = "test-passphrase-123".toCharArray()

        val encrypted = crypto.encrypt(plainBytes, passphrase)
        val decrypted = crypto.decrypt(encrypted, passphrase)

        assertArrayEquals(plainBytes, decrypted)
    }

    @Test
    fun wrongPassphraseThrows() {
        val plainBytes = "sensitive backup data".toByteArray(Charsets.UTF_8)
        val encrypted = crypto.encrypt(plainBytes, "correct-passphrase".toCharArray())

        assertThrows(Exception::class.java) {
            crypto.decrypt(encrypted, "wrong-passphrase".toCharArray())
        }
    }

    @Test
    fun corruptPayloadThrows() {
        val plainBytes = "sensitive backup data".toByteArray(Charsets.UTF_8)
        val encrypted = crypto.encrypt(plainBytes, "test-passphrase".toCharArray())
        encrypted[encrypted.lastIndex] = (encrypted[encrypted.lastIndex].toInt() xor 0xFF).toByte()

        assertThrows(Exception::class.java) {
            crypto.decrypt(encrypted, "test-passphrase".toCharArray())
        }
    }
}
