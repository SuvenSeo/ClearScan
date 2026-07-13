package com.ardeno.clearscan.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MetadataCryptoTest {

    private val crypto = MetadataCrypto(FakeVaultCipher())

    @Test
    fun isPlaintextJson_detectsArrayAndObject() {
        assertTrue(MetadataCrypto.isPlaintextJson("""[{"id":"1"}]"""))
        assertTrue(MetadataCrypto.isPlaintextJson("""  {"id":"1"}  """))
        assertFalse(MetadataCrypto.isPlaintextJson("not-json"))
        assertFalse(MetadataCrypto.isPlaintextJson(""))
    }

    @Test
    fun isEncryptedEnvelope_detectsCsc1Header() {
        val envelope = EncryptedFileStore.packCiphertext(
            VaultCiphertext(iv = ByteArray(12), ciphertext = byteArrayOf(1, 2, 3))
        )
        assertTrue(MetadataCrypto.isEncryptedEnvelope(envelope))
        assertFalse(MetadataCrypto.isEncryptedEnvelope("""[{"id":"1"}]""".encodeToByteArray()))
    }

    @Test
    fun encryptUtf8_decryptUtf8_roundTrip() {
        val plaintext = """[{"id":"doc-1","title":"Scan"}]"""
        val envelope = crypto.encryptUtf8(plaintext)
        assertTrue(MetadataCrypto.isEncryptedEnvelope(envelope))
        assertEquals(plaintext, crypto.decryptUtf8(envelope))
    }

    @Test
    fun encryptPayload_decryptPayload_roundTrip() {
        val json = """{"id":"f-1","name":"Work","createdAt":"2026-01-01T00:00:00Z"}"""
        val encrypted = crypto.encryptPayload(json)
        assertFalse(MetadataCrypto.isPlaintextJson(encrypted))
        assertEquals(json, crypto.decryptPayload(encrypted))
    }

    @Test
    fun decryptPayload_returnsLegacyPlaintextWithoutDecrypting() {
        val legacy = """{"id":"legacy","title":"Old"}"""
        assertEquals(legacy, crypto.decryptPayload(legacy))
    }

    @Test
    fun writeJsonFile_readJsonFile_roundTrip() {
        val dir = Files.createTempDirectory("metadata-crypto").toFile()
        try {
            val file = File(dir, "index.json")
            val json = """[{"id":"1","title":"Test"}]"""
            crypto.writeJsonFile(file, json)
            assertTrue(MetadataCrypto.isEncryptedEnvelope(file.readBytes()))
            assertEquals(json, crypto.readJsonFile(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun readJsonFile_readsLegacyPlaintextFile() {
        val dir = Files.createTempDirectory("metadata-crypto-legacy").toFile()
        try {
            val file = File(dir, "folders.json")
            val json = """[{"id":"f-1","name":"Tax","createdAt":"2026-01-01T00:00:00Z"}]"""
            file.writeText(json)
            assertEquals(json, crypto.readJsonFile(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun encryptPayload_producesDifferentOutputThanPlaintext() {
        val json = """{"id":"1"}"""
        val encrypted = crypto.encryptPayload(json)
        assertNotEquals(json, encrypted)
    }

    private class FakeVaultCipher : VaultCipher {
        override fun ensureVaultKey() = Unit

        override fun encrypt(bytes: ByteArray): VaultCiphertext =
            VaultCiphertext(iv = ByteArray(12), ciphertext = bytes.reversedArray())

        override fun decrypt(ciphertext: VaultCiphertext): ByteArray =
            ciphertext.ciphertext.reversedArray()
    }
}
