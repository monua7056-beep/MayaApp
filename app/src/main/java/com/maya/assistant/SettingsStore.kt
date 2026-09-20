package com.maya.assistant

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "maya_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(v) { prefs.edit().putString(KEY_GEMINI, v).apply() }

    val hasApiKey: Boolean
        get() = geminiApiKey.isNotBlank()

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
    }
}
