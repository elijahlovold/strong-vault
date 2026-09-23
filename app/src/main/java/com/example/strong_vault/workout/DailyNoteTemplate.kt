package com.example.strong_vault.workout

import java.time.LocalDate

/** Minimal skeleton used only when a daily note doesn't exist yet at log time - the normal case
 *  is the user's own daily-note tooling creates the note first. Deliberately minimal frontmatter:
 *  fields like `status`/`energy`/`tags` belong to that other tooling's conventions, which this app
 *  doesn't know and shouldn't guess at. */
object DailyNoteTemplate {

    fun create(date: LocalDate): String = buildString {
        append("---\n")
        append("type: daily\n")
        append("created: $date\n")
        append("---\n")
        append("\n")
        append("# Agenda\n")
        append("\n")
        append("# Scratchpad\n")
        append("\n")
        append("# Workout\n")
        append("\n")
        append("$WORKOUT_END_DELIMITER\n")
    }
}
