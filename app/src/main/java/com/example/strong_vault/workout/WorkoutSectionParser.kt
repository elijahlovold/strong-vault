package com.example.strong_vault.workout

import com.example.strong_vault.workout.model.Load
import com.example.strong_vault.workout.model.WorkoutExercise
import com.example.strong_vault.workout.model.WorkoutSection
import com.example.strong_vault.workout.model.WorkoutSet

/** The required end-of-section marker. Its absence (before EOF or the next top-level heading) is
 *  what turns a "# Workout" section into [WorkoutSectionResult.Malformed] instead of being parsed -
 *  this is the safety net that stops a truncated or hand-edited-into-a-bad-state section from ever
 *  being silently overwritten. */
const val WORKOUT_END_DELIMITER = "<!-- workout:end -->"
const val WORKOUT_HEADING = "# Workout"

sealed class WorkoutSectionResult {
    /** No "# Workout" heading in the note at all. Safe to insert a fresh section. */
    object Absent : WorkoutSectionResult()

    /** Successfully parsed. [headingLineIndex]..[endLineIndex] (inclusive) is the exact line range
     *  spanning "# Workout" through the end delimiter, for splicing on write. */
    data class Parsed(
        val section: WorkoutSection,
        val headingLineIndex: Int,
        val endLineIndex: Int,
    ) : WorkoutSectionResult()

    /** "# Workout" was found but the section could not be safely delimited. The app must never
     *  attempt a structured write against a note in this state. */
    data class Malformed(val reason: String, val headingLineIndex: Int) : WorkoutSectionResult()
}

private val SET_LINE_REGEX = Regex(
    """^-\s*(BW(?:[+-]\d+(?:\.\d+)?)?|\d+(?:\.\d+)?)x(\d+)(?:@(\d+(?:\.\d+)?))?(?:\s+(.+))?$""",
    RegexOption.IGNORE_CASE,
)
private val DAY_LINE_REGEX = Regex("""^\*\*Day\*\*:\s*(.*)$""")
private val EXERCISE_HEADING_REGEX = Regex("""^##\s+(.+?)\s*$""")

object WorkoutSectionParser {

    fun parse(noteText: String): WorkoutSectionResult {
        val lines = noteText.split("\n")
        val headingIndex = lines.indexOfFirst { it.trimEnd('\r') == WORKOUT_HEADING }
        if (headingIndex == -1) return WorkoutSectionResult.Absent

        var endIndex = -1
        for (i in (headingIndex + 1) until lines.size) {
            val line = lines[i].trimEnd('\r')
            if (line == WORKOUT_END_DELIMITER) {
                endIndex = i
                break
            }
            if (line.startsWith("# ") || line == "#") {
                return WorkoutSectionResult.Malformed(
                    reason = "Hit the next section (\"$line\") before finding $WORKOUT_END_DELIMITER",
                    headingLineIndex = headingIndex,
                )
            }
        }
        if (endIndex == -1) {
            return WorkoutSectionResult.Malformed(
                reason = "Reached end of file without finding $WORKOUT_END_DELIMITER",
                headingLineIndex = headingIndex,
            )
        }

        val body = lines.subList(headingIndex + 1, endIndex).map { it.trimEnd('\r') }
        val section = parseBody(body)
        return WorkoutSectionResult.Parsed(section, headingIndex, endIndex)
    }

    private fun parseBody(body: List<String>): WorkoutSection {
        var day: String? = null
        val orphanLines = mutableListOf<String>()
        val exercises = mutableListOf<WorkoutExercise>()

        var currentName: String? = null
        var currentSets: MutableList<WorkoutSet>? = null
        var currentExtra: MutableList<String>? = null

        fun flushCurrent() {
            val name = currentName ?: return
            exercises.add(WorkoutExercise(name, currentSets.orEmpty(), currentExtra.orEmpty()))
            currentName = null
            currentSets = null
            currentExtra = null
        }

        for (raw in body) {
            val line = raw
            if (line.isBlank()) continue

            val exerciseMatch = EXERCISE_HEADING_REGEX.find(line)
            if (exerciseMatch != null) {
                flushCurrent()
                currentName = exerciseMatch.groupValues[1]
                currentSets = mutableListOf()
                currentExtra = mutableListOf()
                continue
            }

            if (currentName == null && day == null) {
                val dayMatch = DAY_LINE_REGEX.find(line)
                if (dayMatch != null) {
                    day = dayMatch.groupValues[1].trim()
                    continue
                }
            }

            if (currentName != null) {
                val set = parseSetLine(line)
                if (set != null) {
                    currentSets!!.add(set)
                } else {
                    currentExtra!!.add(line)
                }
            } else {
                orphanLines.add(line)
            }
        }
        flushCurrent()

        return WorkoutSection(day, orphanLines, exercises)
    }

    /** Parses a single `- <load>x<reps>[@rpe][ note]` line. Public so the UI's manual set-entry
     *  form can accept exactly the same grammar the file format uses, instead of duplicating it. */
    fun parseSetLine(line: String): WorkoutSet? {
        val match = SET_LINE_REGEX.find(line) ?: return null
        val (loadStr, repsStr, rpeStr, note) = match.destructured
        val load = parseLoad(loadStr) ?: return null
        val reps = repsStr.toIntOrNull() ?: return null
        val rpe = rpeStr.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        return WorkoutSet(load, reps, rpe, note.takeIf { it.isNotBlank() })
    }

    private fun parseLoad(token: String): Load? {
        if (token.equals("BW", ignoreCase = true)) return Load.Bodyweight(0.0)
        if (token.startsWith("BW", ignoreCase = true)) {
            val deltaStr = token.substring(2)
            val delta = deltaStr.toDoubleOrNull() ?: return null
            return Load.Bodyweight(delta)
        }
        val weight = token.toDoubleOrNull() ?: return null
        return Load.Absolute(weight)
    }
}
