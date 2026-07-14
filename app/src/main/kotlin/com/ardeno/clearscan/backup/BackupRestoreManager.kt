package com.ardeno.clearscan.backup

import android.content.Context
import android.net.Uri
import com.ardeno.clearscan.data.LocalDocumentRepository
import com.ardeno.clearscan.vault.EncryptedFileStore
import com.ardeno.clearscan.vault.VaultCrypto
import com.ardeno.clearscan.ui.UiStrings
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRestoreManager(
    private val context: Context,
    private val repository: LocalDocumentRepository,
    private val encryptedFileStore: EncryptedFileStore,
    private val vaultCrypto: VaultCrypto,
    private val uiStrings: UiStrings,
    private val passphraseBackupCrypto: PassphraseBackupCrypto = PassphraseBackupCrypto()
) {
    suspend fun exportBackup(
        targetUri: Uri,
        passphrase: CharArray? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()
        if (!repository.hasStoredDocuments()) {
            return@withContext BackupResult.failure(uiStrings.backupNothing())
        }

        val documentsRoot = encryptedFileStore.storageRoot()
        val stagingRoot = File(context.cacheDir, "backup-export-${Instant.now().toEpochMilli()}").apply {
            deleteRecursively()
            mkdirs()
        }

        try {
            repository.writeBackupMetadataFiles(stagingRoot)
            val zipBytes = buildZipBytes(documentsRoot, stagingRoot)
            val version = if (passphrase != null) BACKUP_VERSION_PASSPHRASE else BACKUP_VERSION_DEVICE
            val encryptedPayload = if (passphrase != null) {
                passphraseBackupCrypto.encrypt(zipBytes, passphrase)
            } else {
                EncryptedFileStore.packCiphertext(vaultCrypto.encrypt(zipBytes))
            }

            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                BufferedOutputStream(output).use { stream ->
                    stream.write(BACKUP_MAGIC)
                    stream.write(byteArrayOf(version))
                    stream.write(encryptedPayload)
                }
            } ?: return@withContext BackupResult.failure(uiStrings.backupWriteFailed())

            val message = if (passphrase != null) {
                uiStrings.backupPassphraseSaved()
            } else {
                uiStrings.backupEncryptedSaved()
            }
            BackupResult.success(message)
        } finally {
            stagingRoot.deleteRecursively()
        }
    }

    suspend fun importBackup(
        sourceUri: Uri,
        passphrase: CharArray? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        vaultCrypto.ensureVaultKey()

        val encryptedBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BufferedInputStream(input).readBytes()
        } ?: return@withContext BackupResult.failure(uiStrings.backupReadFailed())

        if (encryptedBytes.size < BACKUP_MAGIC.size + 1) {
            return@withContext BackupResult.failure(uiStrings.backupTooSmall())
        }
        if (!encryptedBytes.copyOfRange(0, BACKUP_MAGIC.size).contentEquals(BACKUP_MAGIC)) {
            return@withContext BackupResult.failure(uiStrings.backupNotClearScan())
        }

        val version = encryptedBytes[BACKUP_MAGIC.size]
        val payload = encryptedBytes.copyOfRange(BACKUP_MAGIC.size + 1, encryptedBytes.size)
        val zipBytes = when (version) {
            BACKUP_VERSION_DEVICE -> runCatching {
                vaultCrypto.decrypt(EncryptedFileStore.unpackCiphertext(payload))
            }.getOrElse {
                return@withContext BackupResult.failure(uiStrings.backupDecryptFailed())
            }
            BACKUP_VERSION_PASSPHRASE -> {
                if (passphrase == null) {
                    return@withContext BackupResult.needsPassphrase(uiStrings)
                }
                runCatching {
                    passphraseBackupCrypto.decrypt(payload, passphrase)
                }.getOrElse {
                    return@withContext BackupResult.failure(uiStrings.backupWrongPassphrase())
                }
            }
            else -> return@withContext BackupResult.failure(uiStrings.backupUnsupportedVersion())
        }

        restoreZipBytes(zipBytes)
    }

    fun backupVersion(sourceUri: Uri): Byte? {
        val header = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BufferedInputStream(input).readNBytes(BACKUP_MAGIC.size + 1)
        } ?: return null
        if (header.size < BACKUP_MAGIC.size + 1) return null
        if (!header.copyOfRange(0, BACKUP_MAGIC.size).contentEquals(BACKUP_MAGIC)) return null
        return header[BACKUP_MAGIC.size]
    }

    private suspend fun restoreZipBytes(zipBytes: ByteArray): BackupResult {
        val stagingDir = File(context.cacheDir, "backup-restore-${Instant.now().toEpochMilli()}").apply {
            deleteRecursively()
            mkdirs()
        }

        return try {
            unzipToDirectory(zipBytes, stagingDir)
            if (!File(stagingDir, "index.json").exists()) {
                BackupResult.failure(uiStrings.backupManifestMissing())
            } else {
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
                repository.invalidateIndexCache()
                val documents = repository.loadDocuments()
                BackupResult.success(uiStrings.backupRestored(documents.size))
            }
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun buildZipBytes(documentsRoot: File, metadataRoot: File): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            metadataRoot.listFiles().orEmpty()
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
                val outFile = safeZipEntryPath(targetDir, entry.name)
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
        val BACKUP_MAGIC = "CSBK".encodeToByteArray()
        const val BACKUP_VERSION_DEVICE: Byte = 1
        const val BACKUP_VERSION_PASSPHRASE: Byte = 2

        @JvmStatic
        fun safeZipEntryPath(targetDir: File, entryName: String): File {
            require(entryName.isNotEmpty()) { "Zip entry name is empty" }
            if (entryName.contains("..")) {
                throw SecurityException("Zip entry contains path traversal: $entryName")
            }
            if (
                entryName.startsWith("/") ||
                entryName.startsWith("\\") ||
                (entryName.length >= 2 && entryName[1] == ':')
            ) {
                throw SecurityException("Zip entry is an absolute path: $entryName")
            }

            val destination = File(targetDir, entryName.replace('\\', '/'))
            val targetCanonical = targetDir.canonicalFile
            val destCanonical = destination.canonicalFile
            val targetPrefix = targetCanonical.path + File.separator
            if (
                destCanonical.path != targetCanonical.path &&
                !destCanonical.path.startsWith(targetPrefix)
            ) {
                throw SecurityException("Zip entry escapes target directory: $entryName")
            }
            return destination
        }
    }
}

data class BackupResult(
    val success: Boolean,
    val message: String,
    val requiresPassphrase: Boolean = false
) {
    companion object {
        fun success(message: String) = BackupResult(success = true, message = message)
        fun failure(message: String) = BackupResult(success = false, message = message)
        fun needsPassphrase(uiStrings: UiStrings) = BackupResult(
            success = false,
            message = uiStrings.backupEnterPassphrase(),
            requiresPassphrase = true
        )
    }
}
