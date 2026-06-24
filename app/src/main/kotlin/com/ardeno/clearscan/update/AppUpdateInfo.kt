package com.ardeno.clearscan.update

import org.json.JSONObject

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val minVersionCode: Int = 1
) {
    fun isNewerThan(installedVersionCode: Int): Boolean {
        return versionCode > installedVersionCode
    }

    fun supportsInstalledVersion(installedVersionCode: Int): Boolean {
        return installedVersionCode >= minVersionCode
    }
}

sealed interface AppUpdateCheckResult {
    data class Available(val info: AppUpdateInfo) : AppUpdateCheckResult
    data class UpToDate(val latestVersionName: String) : AppUpdateCheckResult
    data class Unsupported(val info: AppUpdateInfo) : AppUpdateCheckResult
}

object AppUpdateManifestParser {
    fun parse(json: String): AppUpdateInfo {
        val root = JSONObject(json)
        val versionCode = root.getInt("versionCode")
        val versionName = root.getString("versionName")
        val apkUrl = root.getString("apkUrl")
        val releaseNotes = root.optString("releaseNotes", "Bug fixes and improvements.")
        val minVersionCode = root.optInt("minVersionCode", 1)
        require(versionCode > 0) { "versionCode must be positive." }
        require(apkUrl.startsWith("https://")) { "apkUrl must use HTTPS." }
        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes,
            minVersionCode = minVersionCode
        )
    }
}
