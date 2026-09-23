package com.example.strong_vault.workout

import com.example.strong_vault.workout.model.WorkoutConfig

private const val EXERCISES_HEADING = "# Exercises"
private const val TEMPLATES_HEADING = "# Templates"
private val LIST_ITEM_REGEX = Regex("""^-\s*(.+?)\s*$""")
private val TEMPLATE_HEADING_REGEX = Regex("""^##\s+(.+?)\s*$""")

/**
 * Parses the vault's Workout.md master exercise list + day templates. Deliberately tolerant:
 * an unparseable or missing file just falls back to [WorkoutConfig.DEFAULT] rather than blocking
 * the app, since this file only supplies suggestions/autocomplete, never the source of truth for
 * what was actually logged.
 */
object WorkoutConfigParser {

    fun parse(text: String?): WorkoutConfig {
        if (text == null) return WorkoutConfig.DEFAULT
        val lines = text.split("\n").map { it.trimEnd('\r') }

        val exercises = parseListSection(lines, EXERCISES_HEADING)
        val templates = parseTemplates(lines)

        if (exercises.isEmpty() && templates.isEmpty()) return WorkoutConfig.DEFAULT
        return WorkoutConfig(exercises, templates)
    }

    private fun parseListSection(lines: List<String>, heading: String): List<String> {
        val start = lines.indexOfFirst { it == heading }
        if (start == -1) return emptyList()
        val items = mutableListOf<String>()
        for (i in (start + 1) until lines.size) {
            val line = lines[i]
            if (line.startsWith("# ")) break
            LIST_ITEM_REGEX.find(line)?.let { items.add(it.groupValues[1]) }
        }
        return items
    }

    private fun parseTemplates(lines: List<String>): Map<String, List<String>> {
        val start = lines.indexOfFirst { it == TEMPLATES_HEADING }
        if (start == -1) return emptyMap()
        val templates = linkedMapOf<String, MutableList<String>>()
        var current: MutableList<String>? = null
        for (i in (start + 1) until lines.size) {
            val line = lines[i]
            if (line.startsWith("# ")) break
            val headingMatch = TEMPLATE_HEADING_REGEX.find(line)
            if (headingMatch != null) {
                current = mutableListOf()
                templates[headingMatch.groupValues[1]] = current
                continue
            }
            LIST_ITEM_REGEX.find(line)?.let { current?.add(it.groupValues[1]) }
        }
        return templates
    }

    fun render(config: WorkoutConfig): String {
        val out = mutableListOf<String>()
        out.add("---")
        out.add("type: workout-config")
        out.add("---")
        out.add("")
        out.add(EXERCISES_HEADING)
        out.add("")
        for (exercise in config.exercises) out.add("- $exercise")
        out.add("")
        out.add(TEMPLATES_HEADING)
        out.add("")
        config.templates.entries.forEachIndexed { index, (name, exercises) ->
            out.add("## $name")
            for (exercise in exercises) out.add("- $exercise")
            if (index != config.templates.size - 1) out.add("")
        }
        out.add("")
        return out.joinToString("\n")
    }
}
