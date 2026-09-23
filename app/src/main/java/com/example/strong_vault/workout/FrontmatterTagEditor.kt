package com.example.strong_vault.workout

private const val FENCE = "---"
private val INLINE_TAGS_REGEX = Regex("""^tags:\s*\[(.*)]\s*$""")
private val LIST_ITEM_REGEX = Regex("""^(\s+)-\s*(.+)$""")

/**
 * Adds a tag to a daily note's YAML frontmatter `tags:` list, if it isn't already there.
 * Add-only (never removes a tag) and deliberately conservative: if the frontmatter is missing, has
 * no closing fence, or its `tags:` field isn't in one of the two shapes handled below, the text is
 * returned unchanged rather than guessing at a rewrite of metadata another tool owns.
 */
object FrontmatterTagEditor {

    fun ensureTag(noteText: String, tag: String): String {
        val lines = noteText.split("\n")
        if (lines.isEmpty() || lines[0].trimEnd('\r') != FENCE) return noteText
        val endIndex = (1 until lines.size).firstOrNull { lines[it].trimEnd('\r') == FENCE } ?: return noteText

        val tagsLineIndex = (1 until endIndex).firstOrNull {
            lines[it].trimEnd('\r').trimStart().startsWith("tags:")
        }

        if (tagsLineIndex == null) {
            val newLines = lines.toMutableList()
            newLines.add(endIndex, "tags:")
            newLines.add(endIndex + 1, " - $tag")
            return newLines.joinToString("\n")
        }

        val tagsLine = lines[tagsLineIndex].trimEnd('\r').trim()

        val inlineMatch = INLINE_TAGS_REGEX.find(tagsLine)
        if (inlineMatch != null) {
            val items = inlineMatch.groupValues[1]
                .split(",")
                .map { it.trim().trim('"', '\'') }
                .filter { it.isNotEmpty() }
            if (items.any { it.equals(tag, ignoreCase = true) }) return noteText
            val newLines = lines.toMutableList()
            newLines[tagsLineIndex] = "tags: [${(items + tag).joinToString(", ")}]"
            return newLines.joinToString("\n")
        }

        if (tagsLine != "tags:") return noteText // e.g. "tags: workout" (scalar) - not a shape we rewrite

        var i = tagsLineIndex + 1
        var lastItemIndex = tagsLineIndex
        var indent = " "
        while (i < endIndex) {
            val match = LIST_ITEM_REGEX.find(lines[i].trimEnd('\r')) ?: break
            indent = match.groupValues[1]
            if (match.groupValues[2].trim().equals(tag, ignoreCase = true)) return noteText
            lastItemIndex = i
            i++
        }

        val newLines = lines.toMutableList()
        newLines.add(lastItemIndex + 1, "$indent- $tag")
        return newLines.joinToString("\n")
    }
}
