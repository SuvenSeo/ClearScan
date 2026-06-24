package com.ardeno.clearscan.update

import java.io.File

/** In-memory session for correlating DownloadManager completion with the APK file path. */
object ApkUpdateSession {
    @Volatile
    var pendingDownloadId: Long = -1L

    @Volatile
    var expectedApkFile: File? = null

    fun clear() {
        pendingDownloadId = -1L
        expectedApkFile = null
    }
}
