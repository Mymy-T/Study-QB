package com.olab.studyqb.ui.analyze

import com.olab.studyqb.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the `answerCategory` rule iOS added in `analyzeClues()` on 2026-10-19, which decides
 * what category a card saved straight out of Analyze opens with.
 */
class AnalyzeCategoryTest {

    private val all = Category.all.toSet()

    @Test
    fun `a live suggestion supplies its own category`() {
        assertEquals(
            Category.SCIENCE,
            resolveAnswerCategory(validSuggestion = true, suggestionCategory = Category.SCIENCE, selected = all)
        )
    }

    @Test
    fun `a lone selected category is used when no suggestion is live`() {
        assertEquals(
            Category.MYTHOLOGY,
            resolveAnswerCategory(
                validSuggestion = false,
                suggestionCategory = null,
                selected = setOf(Category.MYTHOLOGY)
            )
        )
    }

    /** The default filter state is all ten, which says nothing about one card's category. */
    @Test
    fun `every category selected yields no category`() {
        assertNull(resolveAnswerCategory(validSuggestion = false, suggestionCategory = null, selected = all))
    }

    @Test
    fun `several but not all categories still yields no category`() {
        assertNull(
            resolveAnswerCategory(
                validSuggestion = false,
                suggestionCategory = null,
                selected = setOf(Category.HISTORY, Category.GEOGRAPHY)
            )
        )
    }

    /**
     * The divergence from iOS. Editing the query clears `validSuggestionMade`, and the stale
     * suggestion category must go with it. iOS clears the flag only *after* launching the
     * lookup, so its next save still lands under the old category.
     */
    @Test
    fun `an invalidated suggestion drops its stale category`() {
        assertNull(
            resolveAnswerCategory(
                validSuggestion = false,
                suggestionCategory = Category.LITERATURE,
                selected = all
            )
        )
    }

    /** A lone filter does not override a live suggestion that disagrees with it. */
    @Test
    fun `a live suggestion outranks a lone selected category`() {
        assertEquals(
            Category.RELIGION,
            resolveAnswerCategory(
                validSuggestion = true,
                suggestionCategory = Category.RELIGION,
                selected = setOf(Category.PHILOSOPHY)
            )
        )
    }
}
