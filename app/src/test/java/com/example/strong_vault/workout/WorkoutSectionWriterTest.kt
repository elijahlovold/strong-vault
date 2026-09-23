package com.example.strong_vault.workout

import com.example.strong_vault.workout.model.Load
import com.example.strong_vault.workout.model.WorkoutExercise
import com.example.strong_vault.workout.model.WorkoutSection
import com.example.strong_vault.workout.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSectionWriterTest {

    @Test
    fun `round trips a parsed section unchanged`() {
        val note = """
            # Agenda

            - stuff

            # Workout

            **Day**: Push

            ## Bench Press
            - 135x8
            - 155x6@8

            <!-- workout:end -->

            # Scratchpad
        """.trimIndent()

        val parsed = WorkoutSectionParser.parse(note) as WorkoutSectionResult.Parsed
        val rewritten = WorkoutSectionWriter.write(note, parsed, parsed.section)
        assertEquals(note, rewritten)
    }

    @Test
    fun `appends a fresh section after Agenda and Scratchpad when absent`() {
        val note = "# Agenda\n\n- stuff\n\n# Scratchpad\n\n- notes\n"
        val result = WorkoutSectionParser.parse(note)
        assertEquals(WorkoutSectionResult.Absent, result)

        val section = WorkoutSection(
            day = "Legs",
            exercises = listOf(
                WorkoutExercise("Squat", listOf(WorkoutSet(Load.Absolute(225.0), 5))),
            ),
        )
        val written = WorkoutSectionWriter.write(note, result, section)

        assertTrue(written.contains("# Workout"))
        assertTrue(written.contains("**Day**: Legs"))
        assertTrue(written.contains("- 225x5"))
        assertTrue(written.contains(WORKOUT_END_DELIMITER))

        val agendaIdx = written.indexOf("# Agenda")
        val workoutIdx = written.indexOf("# Workout")
        val scratchpadIdx = written.indexOf("# Scratchpad")
        assertTrue(agendaIdx < scratchpadIdx)
        assertTrue(scratchpadIdx < workoutIdx)

        val reparsed = WorkoutSectionParser.parse(written)
        assertTrue(reparsed is WorkoutSectionResult.Parsed)
        assertEquals(section, (reparsed as WorkoutSectionResult.Parsed).section)
    }

    @Test
    fun `appends section at end when neither Agenda nor Scratchpad present`() {
        val note = "# Random\n\nhello\n"
        val result = WorkoutSectionParser.parse(note)
        val section = WorkoutSection(exercises = listOf(WorkoutExercise("Squat", listOf(WorkoutSet(Load.Absolute(100.0), 5)))))
        val written = WorkoutSectionWriter.write(note, result, section)
        val reparsed = WorkoutSectionParser.parse(written) as WorkoutSectionResult.Parsed
        assertEquals(section, reparsed.section)
    }

    @Test
    fun `refuses to write into a malformed section`() {
        val note = "# Workout\n\n## Bench Press\n- 135x8\n"
        val result = WorkoutSectionParser.parse(note)
        assertTrue(result is WorkoutSectionResult.Malformed)
        try {
            WorkoutSectionWriter.write(note, result, WorkoutSection())
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `preserves everything outside the Workout section untouched`() {
        val note = """
            ---
            type: daily
            created: 2026-09-22
            ---

            # Agenda

            - important stuff I must not lose

            # Workout

            ## Bench Press
            - 135x8

            <!-- workout:end -->

            # Scratchpad

            - random thoughts
        """.trimIndent()

        val parsed = WorkoutSectionParser.parse(note) as WorkoutSectionResult.Parsed
        val newSection = parsed.section.copy(
            exercises = parsed.section.exercises + WorkoutExercise("Squat", listOf(WorkoutSet(Load.Absolute(225.0), 5))),
        )
        val written = WorkoutSectionWriter.write(note, parsed, newSection)

        assertTrue(written.contains("important stuff I must not lose"))
        assertTrue(written.contains("random thoughts"))
        assertTrue(written.contains("type: daily"))
        assertTrue(written.contains("- 225x5"))
    }

    @Test
    fun `renders bodyweight loads correctly`() {
        assertEquals("- BWx10", WorkoutSectionWriter.renderSetLine(WorkoutSet(Load.Bodyweight(0.0), 10)))
        assertEquals("- BW+25x5", WorkoutSectionWriter.renderSetLine(WorkoutSet(Load.Bodyweight(25.0), 5)))
        assertEquals("- BW-15x12", WorkoutSectionWriter.renderSetLine(WorkoutSet(Load.Bodyweight(-15.0), 12)))
        assertEquals("- 137.5x3@9", WorkoutSectionWriter.renderSetLine(WorkoutSet(Load.Absolute(137.5), 3, 9.0)))
    }
}
