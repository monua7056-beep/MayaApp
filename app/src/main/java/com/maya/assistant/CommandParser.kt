package com.maya.assistant

object CommandParser {

    sealed class Command {
        data class OpenApp(val name: String) : Command()
        data class SearchInApp(val app: String, val query: String) : Command()
        object Back : Command()
        object Home : Command()
        data class Scroll(val dir: String) : Command()
        object ReadScreen : Command()
        data class Unknown(val raw: String) : Command()
    }

    fun parse(raw: String): Command {
        val s = raw.trim()
        val lower = s.lowercase()

        if (lower == "back" || lower.contains("back जाओ") || lower.contains("वापस"))
            return Command.Back
        if (lower == "home" || lower.contains("home पर") || lower.contains("होम"))
            return Command.Home
        if (lower.contains("scroll") && lower.contains("down")) return Command.Scroll("down")
        if (lower.contains("scroll") && lower.contains("up")) return Command.Scroll("up")
        if (lower.contains("screen पढ़ो") || lower.contains("read screen")) return Command.ReadScreen

        val searchRegex = Regex(
            """(.+?)\s*(?:खोलो|open).*?search.*?(?:में|me)?\s*(.+)$""",
            RegexOption.IGNORE_CASE
        )
        searchRegex.find(s)?.let {
            return Command.SearchInApp(it.groupValues[1].trim(), it.groupValues[2].trim())
        }

        val openRegex = Regex("""^(.+?)\s*(?:खोलो|open|खोल|चालू)$""", RegexOption.IGNORE_CASE)
        openRegex.find(s)?.let {
            return Command.OpenApp(it.groupValues[1].trim())
        }

        return Command.Unknown(s)
    }
}
