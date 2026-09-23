package com.example.strong_vault.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class FrontmatterTagEditorTest {

    @Test
    fun `adds tag when tags list exists but is missing it`() {
        val note = "---\ntype: daily\ntags:\n - agenda\n---\n\nbody\n"
        val result = FrontmatterTagEditor.ensureTag(note, "workout")
        assertEquals("---\ntype: daily\ntags:\n - agenda\n - workout\n---\n\nbody\n", result)
    }

    @Test
    fun `no-op when tag already present, case-insensitively`() {
        val note = "---\ntags:\n - Workout\n---\n\nbody\n"
        assertEquals(note, FrontmatterTagEditor.ensureTag(note, "workout"))
    }

    @Test
    fun `adds tags key with item when absent entirely`() {
        val note = "---\ntype: daily\ncreated: 2026-09-22\n---\n\nbody\n"
        val result = FrontmatterTagEditor.ensureTag(note, "workout")
        assertEquals("---\ntype: daily\ncreated: 2026-09-22\ntags:\n - workout\n---\n\nbody\n", result)
    }

    @Test
    fun `adds first item under an empty tags key`() {
        val note = "---\ntags:\nenergy: 5\n---\n\nbody\n"
        val result = FrontmatterTagEditor.ensureTag(note, "workout")
        assertEquals("---\ntags:\n - workout\nenergy: 5\n---\n\nbody\n", result)
    }

    @Test
    fun `handles inline array style`() {
        val note = "---\ntags: [agenda, journal]\n---\n\nbody\n"
        val result = FrontmatterTagEditor.ensureTag(note, "workout")
        assertEquals("---\ntags: [agenda, journal, workout]\n---\n\nbody\n", result)
    }

    @Test
    fun `handles the real daily-note fixture shape untouched when already tagged`() {
        // Mirrors test-daily-note.md's quirky "..." line between the tags list and later keys.
        val note = "---\ntype: daily\nstatus: complete\ncreated: 2026-09-22\ntags:\n - workout\n...\nenergy: 5\n---\n\n# Agenda\n"
        assertEquals(note, FrontmatterTagEditor.ensureTag(note, "workout"))
    }

    @Test
    fun `does nothing without a frontmatter fence`() {
        val note = "# Agenda\n\nno frontmatter here\n"
        assertEquals(note, FrontmatterTagEditor.ensureTag(note, "workout"))
    }

    @Test
    fun `does nothing when closing fence is missing`() {
        val note = "---\ntags:\n - agenda\n\n# Agenda\n"
        assertEquals(note, FrontmatterTagEditor.ensureTag(note, "workout"))
    }

    @Test
    fun `does nothing for a scalar tags value it doesn't recognize`() {
        val note = "---\ntags: workout-log\n---\n\nbody\n"
        assertEquals(note, FrontmatterTagEditor.ensureTag(note, "workout"))
    }
}
