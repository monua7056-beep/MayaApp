package com.maya.assistant

import android.content.Context
import android.content.Intent

class IntentLauncher(private val context: Context) {

    fun openApp(pkg: String): Result<Unit> {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return Result.failure(Exception("Launch intent unavailable"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
