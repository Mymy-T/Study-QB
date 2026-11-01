package com.olab.studyqb.domain

import com.olab.studyqb.data.network.qbreader.dto.FrequencyEntryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the 2026-10-19 iOS change to `suggestTerm()`: a suggestion is now drawn at random from
 * the five most-asked unowned answers rather than always being the single top one, and never
 * repeats the previous suggestion.
 *
 * The pick is random, so the set-based tests draw repeatedly and assert on the set of outcomes.
 * 200 draws from at most five uniform options makes a spurious failure vanishingly unlikely.
 */
class SuggestionPickerTest {

    private fun item(answer: String, count: Int) = FrequencyEntryDto(answer = answer, count = count)

    /** Counts 90, 80, 70 … 20, so the top five are answer1 through answer5. */
    private val ranked = (1..8).map { item("answer$it", 100 - it * 10) }

    private fun drawSet(
        items: List<FrequencyEntryDto> = ranked,
        lastSuggested: String = "",
        isOwned: (String) -> Boolean = { false },
        draws: Int = 200
    ): Set<String> = (1..draws)
        .mapNotNull { SuggestionPicker.pick(items, lastSuggested, isOwned)?.answer }
        .toSet()

    @Test
    fun `draws only from the top five by count`() {
        assertEquals(setOf("answer1", "answer2", "answer3", "answer4", "answer5"), drawSet())
    }

    @Test
    fun `server ordering does not matter`() {
        assertEquals(
            setOf("answer1", "answer2", "answer3", "answer4", "answer5"),
            drawSet(items = ranked.shuffled())
        )
    }

    /** Dropping the top answer promotes the sixth into the pool. */
    @Test
    fun `owned answers are excluded and the pool refills`() {
        assertEquals(
            setOf("answer2", "answer3", "answer4", "answer5", "answer6"),
            drawSet(isOwned = { it == "answer1" })
        )
    }

    @Test
    fun `the previous suggestion is never repeated`() {
        assertEquals(
            setOf("answer1", "answer2", "answer4", "answer5"),
            drawSet(lastSuggested = "answer3")
        )
    }

    @Test
    fun `a sole candidate is returned even if it was the previous suggestion`() {
        val only = listOf(item("Madame Bovary", 42))
        assertEquals("Madame Bovary", SuggestionPicker.pick(only, "Madame Bovary") { false }?.answer)
    }

    /**
     * The reason we don't transcribe iOS's `while !uniqueSuggestion` re-roll: a pool of several
     * entries that all carry the same answer leaves it spinning forever. Here it just repeats.
     */
    @Test
    fun `a pool of duplicate answers repeats instead of hanging`() {
        val duplicates = listOf(item("Krebs cycle", 30), item("Krebs cycle", 20))
        assertEquals("Krebs cycle", SuggestionPicker.pick(duplicates, "Krebs cycle") { false }?.answer)
    }

    @Test
    fun `null when every candidate is already owned`() {
        assertNull(SuggestionPicker.pick(ranked, "") { true })
    }

    @Test
    fun `null for an empty frequency list`() {
        assertNull(SuggestionPicker.pick(emptyList(), "") { false })
    }
}
