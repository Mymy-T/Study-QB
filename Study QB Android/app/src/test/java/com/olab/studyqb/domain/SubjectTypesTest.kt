package com.olab.studyqb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectTypesTest {

    @Test
    fun `all list has exactly 94 entries, matching the transcribed iOS prompt list`() {
        assertEquals(94, SubjectTypes.all.size)
    }

    @Test
    fun `no duplicate subject types`() {
        assertEquals(SubjectTypes.all.size, SubjectTypes.all.toSet().size)
    }

    @Test
    fun `promptListBlock contains every subject type exactly once`() {
        val lines = SubjectTypes.promptListBlock.lines().filter { it.isNotBlank() }
        assertEquals(SubjectTypes.all.toSet(), lines.toSet())
    }

    @Test
    fun `match finds an exact (case-insensitive) response`() {
        assertEquals("composer", SubjectTypes.match("Composer"))
        assertEquals("chemical element", SubjectTypes.match("  chemical element.  "))
    }

    @Test
    fun `match falls back to containment for a slightly verbose response`() {
        assertTrue(SubjectTypes.match("The subject type is: novel") != null)
    }

    @Test
    fun `match returns null for a response matching nothing in the list`() {
        assertEquals(null, SubjectTypes.match("totally unrelated gibberish xyz"))
    }
}
