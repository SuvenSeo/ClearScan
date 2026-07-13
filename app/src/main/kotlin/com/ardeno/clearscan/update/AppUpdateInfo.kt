package com.ardeno.clearscan.update

import org.json.JSONObject

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val minVersionCode: Int = 1,
    val sha256: String? = null
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
        val sha256 = parseSha256(root)
        require(versionCode > 0) { "versionCode must be positive." }
        require(apkUrl.startsWith("https://")) { "apkUrl must use HTTPS." }
        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes,
            minVersionCode = minVersionCode,
            sha256 = sha256
        )
    }

    private fun parseSha256(root: JSONObject): String? {
        if (!root.has("sha256") || root.isNull("sha256")) {
            return null
        }
        val sha256 = root.getString("sha256")
        if (sha256.isBlank()) {
            return null
        }
        require(sha256.length == 64) { "sha256 must be 64 characters." }
        require(sha256.all { it in '0'..'9' || it in 'a'..'f' }) {
            "sha256 must be lowercase hexadecimal."
        }
        return sha256
    }
}
