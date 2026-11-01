package com.olab.studyqb.review

import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.db.entities.FsrsColumns
import com.olab.studyqb.domain.Category
import com.olab.studyqb.ui.review.classic.ClassicPracticeUiState
import com.olab.studyqb.ui.review.classic.ReviewUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Cards store clue lines easiest-first (see CardEntity.personalNotes), but a gradual reveal has
 * to unfold like a real tossup — obscure clue first, giveaway last. PracticeView.swift gets
 * there by calling `clues.reverse()` right after splitting, in both `onAppear` and the Next Card
 * handler; Android was missing it, so slow reveal ran the question backwards.
 */
class ClueRevealOrderTest {

    private val notes = """
        Giveaway: wrote The Old Man and the Sea
        Middle: served as an ambulance driver in WWI
        Hard: a Michigan short story collection features Nick Adams
    """.trimIndent()

    private fun card(personalNotes: String) = CardEntity(
        dateReviewed = Instant.EPOCH,
        answer = "Ernest Hemingway",
        details = "",
        category = Category.LITERATURE,
        personalNotes = personalNotes,
        rgba = "0,0,0,1",
        fsrs = FsrsColumns.new(Instant.EPOCH)
    )

    private fun state(personalNotes: String, sequential: Boolean, revealed: Int = 1) =
        ClassicPracticeUiState(
            loaded = true,
            units = listOf(ReviewUnit(card(personalNotes), personalNotes)),
            revealedLineCount = revealed,
            sequentialClues = sequential
        )

    @Test
    fun `gradual reveal starts from the hardest clue`() {
        val first = state(notes, sequential = true).questionText
        assertEquals("Hard: a Michigan short story collection features Nick Adams", first)
    }

    @Test
    fun `clues unfold toward the giveaway`() {
        val s = state(notes, sequential = true, revealed = 3)
        assertEquals(
            listOf(
                "Hard: a Michigan short story collection features Nick Adams",
                "Middle: served as an ambulance driver in WWI",
                "Giveaway: wrote The Old Man and the Sea"
            ),
            s.questionText.split("\n")
        )
    }

    /** Without sequential mode iOS shows personalNotes verbatim, so order must be untouched. */
    @Test
    fun `non-sequential mode leaves the original order alone`() {
        assertEquals(notes, state(notes, sequential = false).questionText)
    }

    @Test
    fun `clue count is unaffected by reversal`() {
        val s = state(notes, sequential = true)
        assertEquals(3, s.clueLines.size)
        assertEquals(false, s.allCluesRevealed)
        assertEquals(true, state(notes, sequential = true, revealed = 3).allCluesRevealed)
    }

    /** Single-clue mode hands each unit one line, where reversal is a no-op. */
    @Test
    fun `single clue unit is unchanged`() {
        val one = "Middle: served as an ambulance driver in WWI"
        assertEquals(one, state(one, sequential = true).questionText)
    }

    @Test
    fun `blank lines are dropped before reversing`() {
        val padded = "\n\nfirst\n\n  second  \n\n"
        val s = state(padded, sequential = true, revealed = 2)
        assertEquals(listOf("second", "first"), s.questionText.split("\n"))
    }
}
