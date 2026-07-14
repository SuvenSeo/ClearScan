package com.ardeno.clearscan.ui

import android.content.Context
import com.ardeno.clearscan.R
import com.ardeno.clearscan.pdf.PdfCompressQuality

class UiStrings(private val context: Context) {

    // Library
    fun folderCreated(name: String) = context.getString(R.string.msg_folder_created, name)
    fun folderCreateFailed() = context.getString(R.string.msg_folder_create_failed)
    fun folderRenamed(name: String) = context.getString(R.string.msg_folder_renamed, name)
    fun folderRenameFailed() = context.getString(R.string.msg_folder_rename_failed)
    fun folderDeleted() = context.getString(R.string.msg_folder_deleted)
    fun folderDeleteFailed() = context.getString(R.string.msg_folder_delete_failed)
    fun documentsDeleted(count: Int): String = context.resources.getQuantityString(
        R.plurals.msg_documents_deleted,
        count,
        count
    )

    // Vault & settings
    fun vaultEnabledBiometric() = context.getString(R.string.msg_vault_enabled_biometric)
    fun vaultEnabled() = context.getString(R.string.msg_vault_enabled)
    fun vaultDisabled() = context.getString(R.string.msg_vault_disabled)
    fun vaultSetupFailed() = context.getString(R.string.msg_vault_setup_failed)
    fun vaultUnlocked() = context.getString(R.string.msg_vault_unlocked)
    fun vaultKeyMigrationFailed() = context.getString(R.string.msg_vault_key_migration_failed)
    fun vaultLocked() = context.getString(R.string.msg_vault_locked)
    fun selfHostEnabled() = context.getString(R.string.msg_self_host_enabled)
    fun selfHostSaved() = context.getString(R.string.msg_self_host_saved)
    fun ocrBenchmarkRunning() = context.getString(R.string.msg_ocr_benchmark_running)
    fun ocrBenchmarkFinished() = context.getString(R.string.msg_ocr_benchmark_finished)
    fun ocrBenchmarkFailed() = context.getString(R.string.msg_ocr_benchmark_failed)
    fun passphrasesMismatch() = context.getString(R.string.msg_passphrases_mismatch)
    fun appUpToDate(versionName: String) = context.getString(R.string.msg_app_up_to_date, versionName)
    fun appTooOld(versionName: String) = context.getString(R.string.msg_app_too_old, versionName)
    fun updateCheckFailed() = context.getString(R.string.msg_update_check_failed)
    fun downloadingUpdate(versionName: String) = context.getString(R.string.msg_downloading_update, versionName)

    // OCR
    fun ocrFinished(title: String) = context.getString(R.string.msg_ocr_finished, title)
    fun ocrFinishedTags(title: String, tags: String) = context.getString(R.string.msg_ocr_finished_tags, title, tags)
    fun ocrFinishedRedaction(title: String) = context.getString(R.string.msg_ocr_finished_redaction, title)
    fun ocrFailed(title: String) = context.getString(R.string.msg_ocr_failed, title)
    fun ocrAmountTag(amount: String) = context.getString(R.string.msg_ocr_amount_tag, amount)

    // Capture
    fun noFilesSelected() = context.getString(R.string.msg_no_files_selected)
    fun importedPages(count: Int): String = context.resources.getQuantityString(
        R.plurals.msg_imported_pages,
        count,
        count
    )
    fun importFailed() = context.getString(R.string.msg_import_failed)
    fun savedIdScan(pageCount: Int): String = context.resources.getQuantityString(
        R.plurals.msg_saved_id_scan,
        pageCount,
        pageCount
    )
    fun savedDocumentScan(pageCount: Int): String = context.resources.getQuantityString(
        R.plurals.msg_saved_document_scan,
        pageCount,
        pageCount
    )
    fun saveScanFailed() = context.getString(R.string.msg_save_scan_failed)
    fun noPagesCaptured() = context.getString(R.string.msg_no_pages_captured)
    fun savedAutoCapture(pageCount: Int): String = context.resources.getQuantityString(
        R.plurals.msg_saved_auto_capture,
        pageCount,
        pageCount
    )
    fun pageTurnSaveFailed() = context.getString(R.string.msg_page_turn_save_failed)

    // PDF tools
    fun mergeSelectTwo() = context.getString(R.string.msg_merge_select_two)
    fun mergedSelected(count: Int) = context.getString(R.string.msg_merged_selected, count)
    fun mergeNeedTwo() = context.getString(R.string.msg_merge_need_two)
    fun mergedScans(count: Int) = context.getString(R.string.msg_merged_scans, count)
    fun splitDocument(title: String) = context.getString(R.string.msg_split_document, title)
    fun createdRotatedCopy() = context.getString(R.string.msg_created_rotated_copy)
    fun createdSignedCopy() = context.getString(R.string.msg_created_signed_copy)
    fun createdRedactedCopy() = context.getString(R.string.msg_created_redacted_copy)
    fun noIdFieldsToRedact() = context.getString(R.string.msg_no_id_fields_redact)
    fun createdIdRedactedCopy() = context.getString(R.string.msg_created_id_redacted_copy)
    fun createdAnnotatedCopy() = context.getString(R.string.msg_created_annotated_copy)
    fun createdPasswordProtected() = context.getString(R.string.msg_created_password_protected)
    fun createdReorderedCopy() = context.getString(R.string.msg_created_reordered_copy)
    fun createdPagesRemovedCopy() = context.getString(R.string.msg_created_pages_removed)
    fun createdCompressedCopy(quality: PdfCompressQuality): String {
        val qualityLabel = when (quality) {
            PdfCompressQuality.High -> context.getString(R.string.pdf_compress_high)
            PdfCompressQuality.Balanced -> context.getString(R.string.pdf_compress_balanced)
            PdfCompressQuality.Small -> context.getString(R.string.pdf_compress_small)
        }
        return context.getString(R.string.msg_created_compressed_copy, qualityLabel)
    }
    fun pdfToolFailed() = context.getString(R.string.msg_pdf_tool_failed)

    // Document actions
    fun documentRenamed(title: String) = context.getString(R.string.msg_document_renamed, title)
    fun documentRenameFailed() = context.getString(R.string.msg_document_rename_failed)
    fun ocrTextCopied() = context.getString(R.string.msg_ocr_text_copied)
    fun tagsUpdated() = context.getString(R.string.msg_tags_updated)
    fun addedToFavorites() = context.getString(R.string.msg_added_favorites)
    fun removedFromFavorites() = context.getString(R.string.msg_removed_favorites)
    fun movedToLibrary() = context.getString(R.string.msg_moved_to_library)
    fun movedToFolder(folderName: String) = context.getString(R.string.msg_moved_to_folder, folderName)
    fun selectOneDocument() = context.getString(R.string.msg_select_one_document)
    fun selfHostEnableFirst() = context.getString(R.string.msg_self_host_enable_first)
    fun selfHostConfigure() = context.getString(R.string.msg_self_host_configure)
    fun noExportFile() = context.getString(R.string.msg_no_export_file)
    fun exportFileMissing() = context.getString(R.string.msg_export_file_missing)
    fun noPageImages() = context.getString(R.string.msg_no_page_images)
    fun selfHostUploaded(title: String) = context.getString(R.string.msg_self_host_uploaded, title)
    fun selfHostUploadFailed() = context.getString(R.string.msg_self_host_upload_failed)

    // Backup
    fun backupNothing() = context.getString(R.string.msg_backup_nothing)
    fun backupWriteFailed() = context.getString(R.string.msg_backup_write_failed)
    fun backupPassphraseSaved() = context.getString(R.string.msg_backup_passphrase_saved)
    fun backupEncryptedSaved() = context.getString(R.string.msg_backup_encrypted_saved)
    fun backupReadFailed() = context.getString(R.string.msg_backup_read_failed)
    fun backupTooSmall() = context.getString(R.string.msg_backup_too_small)
    fun backupNotClearScan() = context.getString(R.string.msg_backup_not_clearscan)
    fun backupDecryptFailed() = context.getString(R.string.msg_backup_decrypt_failed)
    fun backupWrongPassphrase() = context.getString(R.string.msg_backup_wrong_passphrase)
    fun backupUnsupportedVersion() = context.getString(R.string.msg_backup_unsupported_version)
    fun backupManifestMissing() = context.getString(R.string.msg_backup_manifest_missing)
    fun backupRestored(count: Int): String = context.resources.getQuantityString(
        R.plurals.msg_backup_restored,
        count,
        count
    )
    fun backupEnterPassphrase() = context.getString(R.string.msg_backup_enter_passphrase)

    // MainActivity / share / print / biometric
    fun scannerNoPages() = context.getString(R.string.msg_scanner_no_pages)
    fun scannerUnavailable() = context.getString(R.string.msg_scanner_unavailable)
    fun noOcrText() = context.getString(R.string.msg_no_ocr_text)
    fun noPdfToPrint() = context.getString(R.string.msg_no_pdf_print)
    fun noExportFilesSelected() = context.getString(R.string.msg_no_export_files_selected)
    fun selectedExportsMissing() = context.getString(R.string.msg_selected_exports_missing)
    fun setupScreenLock() = context.getString(R.string.msg_setup_screen_lock)
    fun biometricUnavailable() = context.getString(R.string.msg_biometric_unavailable)
    fun vaultCryptoUnavailable() = context.getString(R.string.msg_vault_crypto_unavailable)
    fun allowInstallUpdates() = context.getString(R.string.msg_allow_install_updates)
    fun chooserShareScan() = context.getString(R.string.chooser_share_scan)
    fun chooserShareImages() = context.getString(R.string.chooser_share_images)
    fun chooserExportOcrText() = context.getString(R.string.chooser_export_ocr_text)
    fun chooserShareSelected() = context.getString(R.string.chooser_share_selected)
    fun biometricEnableVaultTitle() = context.getString(R.string.biometric_enable_vault_title)
    fun biometricEnableVaultSubtitle() = context.getString(R.string.biometric_enable_vault_subtitle)
    fun biometricUnlockTitle() = context.getString(R.string.biometric_unlock_title)
    fun biometricUnlockSubtitle() = context.getString(R.string.biometric_unlock_subtitle)
}
