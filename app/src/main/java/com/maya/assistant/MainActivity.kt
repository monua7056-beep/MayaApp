package com.maya.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private lateinit var apiStatus: TextView
    private lateinit var store: SettingsStore
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = SettingsStore(this)
        output = findViewById(R.id.output)
        apiStatus = findViewById(R.id.apiStatus)

        val input = findViewById<EditText>(R.id.commandInput)
        val runBtn = findViewById<Button>(R.id.runBtn)
        val menuBtn = findViewById<TextView>(R.id.menuBtn)

        menuBtn.setOnClickListener { showMenu() }

        runBtn.setOnClickListener {
            val cmd = input.text.toString().trim()
            if (cmd.isBlank()) return@setOnClickListener
            input.setText("")
            log("> $cmd")
            scope.launch { execute(cmd) }
        }

        updateApiStatus()
        log("🌸 Maya v2 ready. कुछ बोलो या menu (⋮) से Settings खोलो।")
    }

    override fun onResume() {
        super.onResume()
        updateApiStatus()
    }

    private fun updateApiStatus() {
        if (store.hasApiKey) {
            apiStatus.text = "✅ API Key set"
            apiStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            apiStatus.text = "⚠️ API Key not set — menu (⋮) → Settings खोलो"
            apiStatus.setTextColor(0xFFFF9800.toInt())
        }
    }

    private fun showMenu() {
        val options = arrayOf(
            "🔑 API Key Settings",
            "♿ Accessibility Service",
            "🗑️ Clear Chat History",
            "❌ Exit"
        )
        AlertDialog.Builder(this)
            .setTitle("Maya Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, SettingsActivity::class.java))
                    1 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    2 -> { output.text = ""; log("🗑️ Chat cleared.") }
                    3 -> finish()
                }
            }
            .show()
    }

    private suspend fun execute(raw: String) {
        when (val c = CommandParser.parse(raw)) {
            is CommandParser.Command.OpenApp -> handleOpenApp(c.name)
            is CommandParser.Command.SearchInApp -> handleSearch(c.app, c.query)
            is CommandParser.Command.Back -> handleGlobal("back")
            is CommandParser.Command.Home -> handleGlobal("home")
            is CommandParser.Command.Scroll -> handleScroll(c.dir)
            is CommandParser.Command.ReadScreen -> handleReadScreen()
            is CommandParser.Command.Unknown -> {
                if (!store.hasApiKey) {
                    log("❌ समझ नहीं आया। Gemini API key set करो (menu ⋮ → Settings) — फिर Maya कुछ भी समझ सकेगी।")
                } else {
                    log("🤖 (Gemini integration next step में जोड़ेंगे)")
                }
            }
        }
    }

    private suspend fun handleOpenApp(name: String) {
        val app = AppRegistry(this).findApp(name)
        if (app == null) {
            log("❌ मुझे '$name' नहीं मिला।")
            return
        }
        val r = IntentLauncher(this).openApp(app.packageName)
        if (r.isSuccess) log("✅ ${app.name} खोल दिया")
        else log("❌ ${app.name} नहीं खुल पाया")
    }

    private suspend fun handleSearch(appName: String, query: String) {
        handleOpenApp(appName)
        delay(2500)
        val svc = MayaAccessibilityService.instance
        if (svc == null) {
            log("⚠️ Accessibility Service enable नहीं है।")
            return
        }
        val ui = UiInteractor(svc)
        val opened = ui.clickByText("Search") ||
                     ui.clickByText("खोजें") ||
                     ui.clickByResourceId("search") ||
                     ui.clickByResourceId("menu_search")
        if (!opened) {
            log("⚠️ App खोला, search UI नहीं मिला — manual करना पड़ेगा।")
            return
        }
        delay(800)
        val typed = ui.typeIntoFocused(query)
        log(if (typed) "✅ '$query' search में लिख दिया" else "❌ Type नहीं हुआ")
    }

    private fun handleGlobal(which: String) {
        val svc = MayaAccessibilityService.instance
        if (svc == null) { log("⚠️ Accessibility enable नहीं है।"); return }
        val ok = when (which) {
            "back" -> svc.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> svc.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            else -> false
        }
        log(if (ok) "✅ $which done" else "❌ $which fail")
    }

    private fun handleScroll(dir: String) {
        val svc = MayaAccessibilityService.instance
        if (svc == null) { log("⚠️ Accessibility enable नहीं है।"); return }
        val ok = UiInteractor(svc).scroll(dir)
        log(if (ok) "✅ scroll $dir" else "❌ scroll fail")
    }

    private fun handleReadScreen() {
        val svc = MayaAccessibilityService.instance
        if (svc == null) { log("⚠️ Accessibility enable नहीं है।"); return }
        val nodes = ScreenReader(svc).read()
        log("📱 Screen nodes: ${nodes.size}")
        nodes.take(20).forEach { n ->
            val t = n.text ?: n.contentDesc ?: ""
            if (t.isNotBlank()) log("  • $t")
        }
    }

    private fun log(s: String) {
        output.append("$s\n")
    }
}
