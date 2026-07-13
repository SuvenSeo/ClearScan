package com.ardeno.clearscan.update

import java.io.File

/** In-memory session for correlating DownloadManager completion with the APK file path. */
object ApkUpdateSession {
    @Volatile
    var pendingDownloadId: Long = -1L

    @Volatile
    var expectedApkFile: File? = null

    @Volatile
    var expectedSha256: String? = null

    fun clear() {
        pendingDownloadId = -1L
        expectedApkFile = null
        expectedSha256 = null
    }
}
