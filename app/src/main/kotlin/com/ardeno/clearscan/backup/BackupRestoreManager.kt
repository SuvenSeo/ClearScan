package com.ardeno.clearscan.backup

import android.content.Context
import android.net.Uri
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.vault.EncryptedFileStore
import com.ardeno.clearscan.vault.VaultCrypto
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BackupRestoreManager(
    private val context: Context,
    private val repository: LocalDocumentRepository,
    private val encryptedFileStore: EncryptedFileStore,
    private val vaultCrypto: VaultCrypto
) {
    suspend fun exportBackup(targetUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()
        val documentsRoot = encryptedFileStore.storageRoot()
        val indexFile = File(documentsRoot, "index.json")
        if (!indexFile.exists()) {
            return@withContext BackupResult.failure("Nothing to back up yet.")
        }

        val zipBytes = buildZipBytes(documentsRoot, indexFile)
        val encryptedPayload = EncryptedFileStore.packCiphertext(vaultCrypto.encrypt(zipBytes))

        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            BufferedOutputStream(output).use { stream ->
                stream.write(BACKUP_MAGIC)
                stream.write(byteArrayOf(BACKUP_VERSION))
                stream.write(encryptedPayload)
            }
        } ?: return@withContext BackupResult.failure("Could not write backup file.")

        BackupResult.success("Encrypted backup saved.")
    }

    suspend fun importBackup(sourceUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()

        val encryptedBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BufferedInputStream(input).readBytes()
        } ?: return@withContext BackupResult.failure("Could not read backup file.")

        if (encryptedBytes.size < BACKUP_MAGIC.size + 1) {
            return@withContext BackupResult.failure("Backup file is too small.")
        }
        if (!encryptedBytes.copyOfRange(0, BACKUP_MAGIC.size).contentEquals(BACKUP_MAGIC)) {
            return@withContext BackupResult.failure("This is not a ClearScan backup file.")
        }

        val version = encryptedBytes[BACKUP_MAGIC.size]
        if (version != BACKUP_VERSION) {
            return@withContext BackupResult.failure("Unsupported backup version.")
        }

        val payload = encryptedBytes.copyOfRange(BACKUP_MAGIC.size + 1, encryptedBytes.size)
        val zipBytes = runCatching {
            vaultCrypto.decrypt(EncryptedFileStore.unpackCiphertext(payload))
        }.getOrElse {
            return@withContext BackupResult.failure(
                "Could not decrypt backup. Restore only works on the device that created it."
            )
        }

        val stagingDir = File(context.cacheDir, "backup-restore-${Instant.now().toEpochMilli()}").apply {
            deleteRecursively()
            mkdirs()
        }

        try {
            unzipToDirectory(zipBytes, stagingDir)
            if (!File(stagingDir, "index.json").exists()) {
                return@withContext BackupResult.failure("Backup manifest is missing.")
            }

            val documentsRoot = encryptedFileStore.storageRoot()
            documentsRoot.deleteRecursively()
            documentsRoot.mkdirs()

            stagingDir.listFiles().orEmpty().forEach { entry ->
                if (entry.isFile) {
                    entry.copyTo(File(documentsRoot, entry.name), overwrite = true)
                } else {
                    entry.copyRecursively(target = File(documentsRoot, entry.name), overwrite = true)
                }
            }

            encryptedFileStore.clearAllReadableCache()
            val documents = repository.loadDocuments()
            BackupResult.success("Restored ${documents.size} scans from backup.")
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun buildZipBytes(documentsRoot: File, indexFile: File): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            documentsRoot.listFiles().orEmpty()
                .filter { it.isFile && (it.name == "index.json" || it.name == "folders.json") }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }

            documentsRoot.listFiles().orEmpty()
                .filter { it.isDirectory }
                .forEach { documentDir ->
                    documentDir.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val relative = file.relativeTo(documentsRoot).path.replace('\\', '/')
                            zip.putNextEntry(ZipEntry(relative))
                            file.inputStream().use { input -> input.copyTo(zip) }
                            zip.closeEntry()
                        }
                }
        }
        return buffer.toByteArray()
    }

    private fun unzipToDirectory(zipBytes: ByteArray, targetDir: File) {
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    companion object {
        private val BACKUP_MAGIC = "CSBK".encodeToByteArray()
        private const val BACKUP_VERSION: Byte = 1
    }
}

data class BackupResult(
    val success: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = BackupResult(success = true, message = message)
        fun failure(message: String) = BackupResult(success = false, message = message)
    }
}
