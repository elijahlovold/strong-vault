package com.example.strong_vault.workout

import com.example.strong_vault.workout.model.Load
import com.example.strong_vault.workout.model.WorkoutExercise
import com.example.strong_vault.workout.model.WorkoutSection
import com.example.strong_vault.workout.model.WorkoutSet

private const val AGENDA_HEADING = "# Agenda"
private const val SCRATCHPAD_HEADING = "# Scratchpad"

object WorkoutSectionWriter {

    /**
     * Returns [noteText] with its Workout section replaced (or freshly inserted) by [section].
     * Only valid to call when [result] is [WorkoutSectionResult.Parsed] or [WorkoutSectionResult.Absent] -
     * callers must never attempt this against a [WorkoutSectionResult.Malformed] note.
     */
    fun write(noteText: String, result: WorkoutSectionResult, section: WorkoutSection): String {
        require(result !is WorkoutSectionResult.Malformed) {
            "Refusing to write a structured Workout section into a malformed note"
        }
        val lines = noteText.split("\n").toMutableList()
        val newBlock = renderBlock(section)

        when (result) {
            is WorkoutSectionResult.Parsed -> {
                val before = lines.subList(0, result.headingLineIndex)
                val after = lines.subList(result.endLineIndex + 1, lines.size)
                return (before + newBlock + after).joinToString("\n")
            }
            WorkoutSectionResult.Absent -> {
                val insertAt = insertionIndex(lines)
                val before = lines.subList(0, insertAt)
                val after = lines.subList(insertAt, lines.size)
                val spacedBlock = if (before.isNotEmpty() && before.last().isNotBlank()) {
                    listOf("") + newBlock
                } else {
                    newBlock
                }
                val spacedAfter = if (after.isNotEmpty() && after.first().isNotBlank()) {
                    listOf("") + after
                } else {
                    after
                }
                return (before + spacedBlock + spacedAfter).joinToString("\n")
            }
            is WorkoutSectionResult.Malformed -> error("unreachable")
        }
    }

    private fun insertionIndex(lines: List<String>): Int {
        val agendaIndex = lines.indexOfFirst { it.trimEnd('\r') == AGENDA_HEADING }
        if (agendaIndex != -1) {
            var i = agendaIndex + 1
            while (i < lines.size && !isTopLevelHeading(lines[i])) i++
            return i
        }
        val scratchpadIndex = lines.indexOfFirst { it.trimEnd('\r') == SCRATCHPAD_HEADING }
        if (scratchpadIndex != -1) return scratchpadIndex
        return lines.size
    }

    private fun isTopLevelHeading(line: String): Boolean {
        val trimmed = line.trimEnd('\r')
        return trimmed.startsWith("# ") || trimmed == "#"
    }

    private fun renderBlock(section: WorkoutSection): List<String> {
        val out = mutableListOf<String>()
        out.add(WORKOUT_HEADING)
        out.add("")
        if (section.day != null) {
            out.add("**Day**: ${section.day}")
        }
        for (line in section.orphanLines) out.add(line)
        if ((section.day != null || section.orphanLines.isNotEmpty()) && section.exercises.isNotEmpty()) {
            out.add("")
        }
        section.exercises.forEachIndexed { index, exercise ->
            out.add("## ${exercise.name}")
            for (set in exercise.sets) out.add(renderSetLine(set))
            for (line in exercise.extraLines) out.add(line)
            if (index != section.exercises.lastIndex) out.add("")
        }
        if (section.exercises.isNotEmpty()) out.add("")
        out.add(WORKOUT_END_DELIMITER)
        return out
    }

    fun renderSetLine(set: WorkoutSet): String {
        val loadStr = renderLoad(set.load)
        val rpeStr = set.rpe?.let { "@${formatNumber(it)}" } ?: ""
        val noteStr = set.note?.let { " $it" } ?: ""
        return "- $loadStr" + "x${set.reps}" + rpeStr + noteStr
    }

    private fun renderLoad(load: Load): String = when (load) {
        is Load.Absolute -> formatNumber(load.weight)
        is Load.Bodyweight -> when {
            load.delta == 0.0 -> "BW"
            load.delta > 0 -> "BW+${formatNumber(load.delta)}"
            else -> "BW-${formatNumber(-load.delta)}"
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
