package com.maya.assistant

import android.content.Context
import android.content.Intent

class AppRegistry(private val context: Context) {

    data class AppInfo(
        val name: String,
        val packageName: String,
        val launchable: Boolean
    )

    fun discover(): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(mainIntent, 0).map { ri ->
            AppInfo(
                name = ri.loadLabel(pm).toString(),
                packageName = ri.activityInfo.packageName,
                launchable = pm.getLaunchIntentForPackage(ri.activityInfo.packageName) != null
            )
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }

    fun findApp(query: String): AppInfo? {
        val q = query.lowercase().trim()
        val all = discover()
        return all.firstOrNull { it.name.lowercase() == q }
            ?: all.firstOrNull { it.name.lowercase().contains(q) }
            ?: all.firstOrNull { it.packageName.lowercase().contains(q) }
    }
}
