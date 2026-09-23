package com.example.strong_vault.workout

import com.example.strong_vault.vault.VaultFileIo
import com.example.strong_vault.vault.VaultPaths
import com.example.strong_vault.workout.model.WorkoutConfig
import com.example.strong_vault.workout.model.WorkoutSection
import java.io.File
import java.io.IOException
import java.time.LocalDate

/** A loaded day: the raw note text (null if the note doesn't exist yet) plus how its Workout
 *  section parsed. [result] and, when parseable, the derived section are the "base" state a save
 *  must be diffed against to detect a concurrent external edit. */
data class WorkoutDayState(
    val date: LocalDate,
    val noteExists: Boolean,
    val result: WorkoutSectionResult,
)

data class WorkoutDaySummary(
    val date: LocalDate,
    val day: String?,
    val exerciseCount: Int,
    val setCount: Int,
)

sealed class SaveOutcome {
    object Success : SaveOutcome()

    /** The note's Workout section changed externally since it was loaded. [freshSection] is what
     *  it now contains (null if the note itself vanished) - the caller must reload and let the
     *  user decide, rather than the app silently overwriting someone else's concurrent edit. */
    data class Conflict(val freshSection: WorkoutSection?) : SaveOutcome()

    /** The note's Workout section can't be safely written to (missing end delimiter, etc). */
    data class Malformed(val reason: String) : SaveOutcome()

    data class IoError(val message: String) : SaveOutcome()
}

class WorkoutRepository(private val vaultRoot: File) {

    fun loadDay(date: LocalDate): WorkoutDayState {
        val file = VaultPaths.dailyNoteFile(vaultRoot, date)
        val text = VaultFileIo.readTextOrNull(file)
        val result = if (text == null) WorkoutSectionResult.Absent else WorkoutSectionParser.parse(text)
        return WorkoutDayState(date, noteExists = text != null, result = result)
    }

    fun baseSectionOf(result: WorkoutSectionResult): WorkoutSection = when (result) {
        is WorkoutSectionResult.Parsed -> result.section
        WorkoutSectionResult.Absent -> WorkoutSection()
        is WorkoutSectionResult.Malformed -> WorkoutSection()
    }

    /**
     * Writes [updatedSection] for [date]. Re-reads and re-parses the note fresh right before
     * writing (never trusting a stale in-memory copy) and compares the freshly parsed section
     * against [base] - the section the edit session started from. A mismatch means something else
     * changed the Workout section since load, so this returns [SaveOutcome.Conflict] instead of
     * overwriting it.
     */
    fun save(
        date: LocalDate,
        base: WorkoutDayState,
        updatedSection: WorkoutSection,
    ): SaveOutcome {
        val file = VaultPaths.dailyNoteFile(vaultRoot, date)
        return try {
            val currentText = VaultFileIo.readTextOrNull(file)
            if (currentText == null) {
                if (base.noteExists) {
                    return SaveOutcome.Conflict(null)
                }
                val fresh = DailyNoteTemplate.create(date)
                val freshResult = WorkoutSectionParser.parse(fresh)
                val written = WorkoutSectionWriter.write(fresh, freshResult, updatedSection)
                VaultFileIo.writeTextAtomically(file, withWorkoutTag(written, updatedSection))
                return SaveOutcome.Success
            }

            val freshResult = WorkoutSectionParser.parse(currentText)
            if (freshResult is WorkoutSectionResult.Malformed) {
                return SaveOutcome.Malformed(freshResult.reason)
            }
            val freshSection = baseSectionOf(freshResult)
            val baseSection = baseSectionOf(base.result)
            if (base.result is WorkoutSectionResult.Malformed || freshSection != baseSection) {
                return SaveOutcome.Conflict(freshSection)
            }

            val written = WorkoutSectionWriter.write(currentText, freshResult, updatedSection)
            VaultFileIo.writeTextAtomically(file, withWorkoutTag(written, updatedSection))
            SaveOutcome.Success
        } catch (e: IOException) {
            SaveOutcome.IoError(e.message ?: "IO error")
        } catch (e: SecurityException) {
            SaveOutcome.IoError(e.message ?: "Permission denied")
        }
    }

    /** Only adds the tag - a workout emptied back out (all exercises removed) keeps whatever tag
     *  was already there, since removing it could just as easily strip a tag the user added by
     *  hand for an unrelated reason. */
    private fun withWorkoutTag(noteText: String, section: WorkoutSection): String =
        if (section.exercises.isNotEmpty()) FrontmatterTagEditor.ensureTag(noteText, "workout") else noteText

    fun loadConfig(): WorkoutConfig {
        val file = VaultPaths.workoutConfigFile(vaultRoot)
        return WorkoutConfigParser.parse(VaultFileIo.readTextOrNull(file))
    }

    /** True only if Workout.md is absent - used to gate an explicit "create default Workout.md"
     *  action; never overwrites an existing (even malformed) file. */
    fun canCreateDefaultConfig(): Boolean =
        !VaultPaths.workoutConfigFile(vaultRoot).isFile

    fun writeDefaultConfig() {
        val file = VaultPaths.workoutConfigFile(vaultRoot)
        if (file.isFile) return
        VaultFileIo.writeTextAtomically(file, WorkoutConfigParser.render(WorkoutConfig.DEFAULT))
    }

    /** Scans backwards from [through] for [days] calendar days, parsing each note found and
     *  summarizing any well-formed Workout section. Bounded and lazy by design - never scans the
     *  whole vault - so it stays fast regardless of how long the vault has existed. Malformed or
     *  missing notes are silently skipped here; they're only surfaced when the user opens that
     *  specific day. */
    fun recentSummaries(through: LocalDate, days: Int): List<WorkoutDaySummary> {
        val summaries = mutableListOf<WorkoutDaySummary>()
        for (offset in 0 until days) {
            val date = through.minusDays(offset.toLong())
            val file = VaultPaths.dailyNoteFile(vaultRoot, date)
            val text = VaultFileIo.readTextOrNull(file) ?: continue
            val result = WorkoutSectionParser.parse(text)
            if (result is WorkoutSectionResult.Parsed && result.section.exercises.isNotEmpty()) {
                summaries.add(
                    WorkoutDaySummary(
                        date = date,
                        day = result.section.day,
                        exerciseCount = result.section.exercises.size,
                        setCount = result.section.exercises.sumOf { it.sets.size },
                    ),
                )
            }
        }
        return summaries
    }

    /** Same bounded backward scan as [recentSummaries], but returns full parsed sections keyed by
     *  date for statistics (progression charts, PRs) over a given exercise. */
    fun recentSections(through: LocalDate, days: Int): List<Pair<LocalDate, WorkoutSection>> {
        val sections = mutableListOf<Pair<LocalDate, WorkoutSection>>()
        for (offset in 0 until days) {
            val date = through.minusDays(offset.toLong())
            val file = VaultPaths.dailyNoteFile(vaultRoot, date)
            val text = VaultFileIo.readTextOrNull(file) ?: continue
            val result = WorkoutSectionParser.parse(text)
            if (result is WorkoutSectionResult.Parsed && result.section.exercises.isNotEmpty()) {
                sections.add(date to result.section)
            }
        }
        return sections
    }
}
