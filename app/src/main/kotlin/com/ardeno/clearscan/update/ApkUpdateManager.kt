package com.ardeno.clearscan.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.ardeno.clearscan.BuildConfig
import java.io.File

class ApkUpdateManager(
    private val context: Context,
    private val updateChecker: UpdateChecker = UpdateChecker(BuildConfig.UPDATE_MANIFEST_URL)
) {
    private val downloadManager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val installedVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val installedVersionName: String
        get() = BuildConfig.VERSION_NAME

    suspend fun checkForUpdate(): Result<AppUpdateCheckResult> {
        return updateChecker.fetchLatest().map { manifest ->
            when {
                !manifest.supportsInstalledVersion(installedVersionCode) ->
                    AppUpdateCheckResult.Unsupported(manifest)
                manifest.isNewerThan(installedVersionCode) ->
                    AppUpdateCheckResult.Available(manifest)
                else ->
                    AppUpdateCheckResult.UpToDate(manifest.versionName)
            }
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createInstallPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun enqueueDownload(update: AppUpdateInfo): Long {
        val destination = updateApkFile()
        destination.parentFile?.mkdirs()
        if (destination.exists()) {
            destination.delete()
        }

        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("ClearScan ${update.versionName}")
            .setDescription("Downloading update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                UPDATE_APK_FILE_NAME
            )

        val downloadId = downloadManager.enqueue(request)
        ApkUpdateSession.pendingDownloadId = downloadId
        ApkUpdateSession.expectedApkFile = destination
        return downloadId
    }

    fun launchInstaller(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun updateApkFile(): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            UPDATE_APK_FILE_NAME
        )
    }

    companion object {
        const val UPDATE_APK_FILE_NAME = "clearscan-update.apk"
    }
}
