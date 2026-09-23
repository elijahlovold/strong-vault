package com.example.strong_vault.workout

import com.example.strong_vault.workout.model.Load
import com.example.strong_vault.workout.model.WorkoutExercise
import com.example.strong_vault.workout.model.WorkoutSection
import com.example.strong_vault.workout.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

class WorkoutRepositoryTest {

    private fun tempVault(): File = Files.createTempDirectory("strong-vault-test").toFile()

    @Test
    fun `saving a workout with sets adds the workout tag to a note that lacks tags`() {
        val vault = tempVault()
        val date = LocalDate.of(2026, 9, 22)
        val noteFile = File(vault, "Calendar/${date.year}/$date.md")
        noteFile.parentFile!!.mkdirs()
        noteFile.writeText("---\ntype: daily\ncreated: $date\n---\n\n# Agenda\n\n# Scratchpad\n")

        val repo = WorkoutRepository(vault)
        val base = repo.loadDay(date)
        val section = WorkoutSection(
            day = "Push",
            exercises = listOf(WorkoutExercise("Bench Press", listOf(WorkoutSet(Load.Absolute(135.0), 8)))),
        )

        val outcome = repo.save(date, base, section)
        assertEquals(SaveOutcome.Success, outcome)

        val text = noteFile.readText()
        assertTrue(text.contains("tags:\n - workout"))
        assertTrue(text.contains("- 135x8"))
    }

    @Test
    fun `saving an empty workout section does not add the tag`() {
        val vault = tempVault()
        val date = LocalDate.of(2026, 9, 22)
        val noteFile = File(vault, "Calendar/${date.year}/$date.md")
        noteFile.parentFile!!.mkdirs()
        noteFile.writeText("---\ntype: daily\ncreated: $date\n---\n\n# Agenda\n\n# Scratchpad\n")

        val repo = WorkoutRepository(vault)
        val base = repo.loadDay(date)
        val outcome = repo.save(date, base, WorkoutSection())

        assertEquals(SaveOutcome.Success, outcome)
        assertFalse(noteFile.readText().contains("tags:"))
    }

    @Test
    fun `creating a brand new note also gets the tag when it has sets`() {
        val vault = tempVault()
        val date = LocalDate.of(2026, 9, 22)
        val repo = WorkoutRepository(vault)
        val base = repo.loadDay(date)
        assertFalse(base.noteExists)

        val section = WorkoutSection(exercises = listOf(WorkoutExercise("Squat", listOf(WorkoutSet(Load.Absolute(225.0), 5)))))
        val outcome = repo.save(date, base, section)

        assertEquals(SaveOutcome.Success, outcome)
        val text = File(vault, "Calendar/${date.year}/$date.md").readText()
        assertTrue(text.contains("tags:\n - workout"))
    }
}
