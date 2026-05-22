package com.example.crewportal.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppUpdateInfo(
    val latestVersion: String,
    val versionCode: Int,
    val apkUrl: String,
    val changelog: List<String>
)

class UpdateRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val updateUrl = "https://raw.githubusercontent.com/Dmitry-Alekseev/CrewPortal/main/update/app_update.json"

    fun checkForUpdate(): AppUpdateInfo? {
        return try {
            val response = client.newCall(Request.Builder().url(updateUrl).build()).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) return null
            val root = JSONObject(body)
            val changes = root.optJSONArray("changelog")
            val list = buildList {
                if (changes != null) for (i in 0 until changes.length()) add(changes.getString(i))
            }
            AppUpdateInfo(
                latestVersion = root.optString("latestVersion", ""),
                versionCode = root.optInt("versionCode", 0),
                apkUrl = root.optString("apkUrl", ""),
                changelog = list
            )
        } catch (_: Exception) {
            null
        }
    }

    fun openDownload(url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
