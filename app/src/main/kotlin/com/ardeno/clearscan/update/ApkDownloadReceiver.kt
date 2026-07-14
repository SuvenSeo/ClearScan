package com.ardeno.clearscan.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ardeno.clearscan.R

class ApkDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            return
        }

        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (completedId != ApkUpdateSession.pendingDownloadId) {
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(completedId)
        val cursor = downloadManager.query(query)
        cursor.use {
            if (!it.moveToFirst()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.update_download_not_found),
                    Toast.LENGTH_LONG
                ).show()
                ApkUpdateSession.clear()
                return
            }

            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(
                    context,
                    context.getString(R.string.update_download_failed),
                    Toast.LENGTH_LONG
                ).show()
                ApkUpdateSession.clear()
                return
            }
        }

        val apkFile = ApkUpdateSession.expectedApkFile
        val expectedSha256 = ApkUpdateSession.expectedSha256
        ApkUpdateSession.clear()

        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(
                context,
                context.getString(R.string.update_apk_missing),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!ApkIntegrityVerifier.verify(apkFile, expectedSha256)) {
            apkFile.delete()
            Toast.makeText(
                context,
                context.getString(R.string.update_apk_integrity_failed),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val manager = ApkUpdateManager(context.applicationContext)
        if (!manager.canInstallPackages()) {
            Toast.makeText(
                context,
                context.getString(R.string.update_allow_install_retry),
                Toast.LENGTH_LONG
            ).show()
            context.startActivity(manager.createInstallPermissionIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }

        manager.launchInstaller(apkFile)
    }
}
