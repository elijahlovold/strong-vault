package com.example.strong_vault.workout

import com.example.strong_vault.workout.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSectionParserTest {

    private val sampleNote = """
        ---
        type: daily
        created: 2026-09-22
        ---

        # Agenda

        - morning agenda

        # Workout

        **Day**: Push

        ## Bench Press
        - 135x8
        - 155x6
        - 165x5@8

        ## Overhead Press
        - 95x8
        - 95x8
        - 95x6@9 felt heavy

        <!-- workout:end -->

        # Scratchpad

    """.trimIndent()

    @Test
    fun `parses a well-formed section`() {
        val result = WorkoutSectionParser.parse(sampleNote) as WorkoutSectionResult.Parsed
        val section = result.section
        assertEquals("Push", section.day)
        assertEquals(2, section.exercises.size)

        val bench = section.exercises[0]
        assertEquals("Bench Press", bench.name)
        assertEquals(3, bench.sets.size)
        assertEquals(Load.Absolute(135.0), bench.sets[0].load)
        assertEquals(8, bench.sets[0].reps)
        assertNull(bench.sets[0].rpe)
        assertEquals(8.0, bench.sets[2].rpe)

        val ohp = section.exercises[1]
        assertEquals("felt heavy", ohp.sets[2].note)
    }

    @Test
    fun `missing heading is absent`() {
        val note = "# Agenda\n\nsomething\n\n# Scratchpad\n"
        assertEquals(WorkoutSectionResult.Absent, WorkoutSectionParser.parse(note))
    }

    @Test
    fun `missing end delimiter before next heading is malformed`() {
        val note = "# Workout\n\n**Day**: Push\n\n## Bench Press\n- 135x8\n\n# Scratchpad\n"
        val result = WorkoutSectionParser.parse(note)
        assertTrue(result is WorkoutSectionResult.Malformed)
    }

    @Test
    fun `missing end delimiter at eof is malformed`() {
        val note = "# Workout\n\n**Day**: Push\n\n## Bench Press\n- 135x8\n"
        val result = WorkoutSectionParser.parse(note)
        assertTrue(result is WorkoutSectionResult.Malformed)
    }

    @Test
    fun `bodyweight loads parse correctly`() {
        val note = """
            # Workout

            ## Pull-up
            - BWx10
            - BW+25x5
            - BW-15x12

            <!-- workout:end -->
        """.trimIndent()
        val result = WorkoutSectionParser.parse(note) as WorkoutSectionResult.Parsed
        val sets = result.section.exercises[0].sets
        assertEquals(Load.Bodyweight(0.0), sets[0].load)
        assertEquals(Load.Bodyweight(25.0), sets[1].load)
        assertEquals(Load.Bodyweight(-15.0), sets[2].load)
    }

    @Test
    fun `unrecognized lines under an exercise are preserved as extra lines, not dropped`() {
        val note = """
            # Workout

            ## Bench Press
            - 135x8
            felt off today, shoulder tight
            - 155x6

            <!-- workout:end -->
        """.trimIndent()
        val result = WorkoutSectionParser.parse(note) as WorkoutSectionResult.Parsed
        val bench = result.section.exercises[0]
        assertEquals(2, bench.sets.size)
        assertEquals(listOf("felt off today, shoulder tight"), bench.extraLines)
    }

    @Test
    fun `orphan lines before first exercise are preserved`() {
        val note = """
            # Workout

            **Day**: Push
            supersetting today

            ## Bench Press
            - 135x8

            <!-- workout:end -->
        """.trimIndent()
        val result = WorkoutSectionParser.parse(note) as WorkoutSectionResult.Parsed
        assertEquals(listOf("supersetting today"), result.section.orphanLines)
    }
}
