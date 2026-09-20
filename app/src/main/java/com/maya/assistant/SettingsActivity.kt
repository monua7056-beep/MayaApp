package com.maya.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val apiInput = findViewById<EditText>(R.id.apiKeyInput)
        val saveBtn = findViewById<Button>(R.id.saveKeyBtn)
        val statusTxt = findViewById<TextView>(R.id.keyStatus)
        val accessBtn = findViewById<Button>(R.id.accessibilityBtn)

        val store = SettingsStore(this)

        // Show saved key (masked)
        if (store.hasApiKey) {
            apiInput.setText(store.geminiApiKey)
            statusTxt.text = "✅ API Key saved"
            statusTxt.setTextColor(0xFF4CAF50.toInt())
        } else {
            statusTxt.text = "⚠️ API Key not set"
            statusTxt.setTextColor(0xFFFF9800.toInt())
        }

        saveBtn.setOnClickListener {
            val key = apiInput.text.toString().trim()
            if (key.isBlank()) {
                statusTxt.text = "❌ खाली key save नहीं होगी"
                statusTxt.setTextColor(0xFFF44336.toInt())
                return@setOnClickListener
            }
            if (!key.startsWith("AIza")) {
                statusTxt.text = "⚠️ Gemini API key आमतौर पर 'AIza' से शुरू होती है"
                statusTxt.setTextColor(0xFFFF9800.toInt())
            }
            store.geminiApiKey = key
            statusTxt.text = "✅ API Key saved successfully!"
            statusTxt.setTextColor(0xFF4CAF50.toInt())
        }

        accessBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}
