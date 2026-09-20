package com.maya.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.commandInput)
        val runBtn = findViewById<Button>(R.id.runBtn)
        val accessBtn = findViewById<Button>(R.id.accessBtn)
        output = findViewById(R.id.output)

        accessBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        runBtn.setOnClickListener {
            val cmd = input.text.toString()
            if (cmd.isBlank()) return@setOnClickListener
            log("> $cmd")
            scope.launch { execute(cmd) }
        }
    }

    private suspend fun execute(raw: String) {
        when (val c = CommandParser.parse(raw)) {
            is CommandParser.Command.OpenApp -> handleOpenApp(c.name)
            is CommandParser.Command.SearchInApp -> handleSearch(c.app, c.query)
            is CommandParser.Command.Back -> handleGlobal("back")
            is CommandParser.Command.Home -> handleGlobal("home")
            is CommandParser.Command.Scroll -> handleScroll(c.dir)
            is CommandParser.Command.ReadScreen -> handleReadScreen()
            is CommandParser.Command.Unknown -> log("❌ समझ नहीं आया: ${c.raw}")
        }
    }

    private suspend fun handleOpenApp(name: String) {
        val reg = AppRegistry(this)
        val app = reg.findApp(name)
        if (app == null) {
            log("❌ मुझे आपके फोन में '$name' नहीं मिला।")
            return
        }
        val r = IntentLauncher(this).openApp(app.packageName)
        if (r.isSuccess) log("✅ ${app.name} खोल दिया (${app.packageName})")
        else log("❌ ${app.name} नहीं खुल पाया: ${r.exceptionOrNull()?.message}")
    }

    private suspend fun handleSearch(appName: String, query: String) {
        handleOpenApp(appName)
        delay(2500)

        val svc = MayaAccessibilityService.instance
        if (svc == null) {
            log("⚠️ Accessibility Service enable नहीं है।")
            log("   'Accessibility Settings' button दबाओ और Maya को enable करो।")
            return
        }

        val ui = UiInteractor(svc)
        val opened = ui.clickByText("Search") ||
                     ui.clickByText("खोजें") ||
                     ui.clickByResourceId("search") ||
                     ui.clickByResourceId("menu_search")

        if (!opened) {
            log("⚠️ यह app search UI expose नहीं करता। App खोला है, बाकी manual करो।")
            return
        }
        delay(800)
        val typed = ui.typeIntoFocused(query)
        log(if (typed) "✅ Search में '$query' लिख दिया" else "❌ Type नहीं हो पाया")
    }

    private fun handleGlobal(which: String) {
        val svc = MayaAccessibilityService.instance
        if (svc == null) { log("⚠️ Accessibility Service enable नहीं है।"); return }
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
        if (svc == null) { log("⚠️ Accessibility Service enable नहीं है।"); return }
        val ok = UiInteractor(svc).scroll(dir)
        log(if (ok) "✅ scroll $dir" else "❌ scroll fail")
    }

    private fun handleReadScreen() {
        val svc = MayaAccessibilityService.instance
        if (svc == null) { log("⚠️ Accessibility Service enable नहीं है।"); return }
        val nodes = ScreenReader(svc).read()
        log("📱 Screen nodes: ${nodes.size}")
        nodes.take(40).forEach { n ->
            val t = n.text ?: n.contentDesc ?: ""
            if (t.isNotBlank()) log("  • $t")
        }
    }

    private fun log(s: String) {
        output.append("$s\n")
    }
}
