package com.olab.studyqb.domain

import com.olab.studyqb.ui.analyze.AnalyzeUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the Analyze tab's opening filter state against AnalyzeView.swift, where
 * `difficulties` starts as the literal `[2, 3, 4, 5]` — the High School band. Defaulting to all
 * eleven levels instead pulls middle-school and open-college questions into a high-school
 * player's first search.
 */
class AnalyzeDefaultsTest {

    @Test
    fun `difficulties default to the High School band`() {
        assertEquals(setOf(2, 3, 4, 5), AnalyzeUiState().difficulties)
    }

    /** Must stay the same object the "High School" menu preset applies, or the two can drift. */
    @Test
    fun `the default is exactly the High School preset`() {
        assertEquals(Difficulty.highSchool, AnalyzeUiState().difficulties)
    }

    /** iOS defaults categories to every category; only difficulty is narrowed. */
    @Test
    fun `categories still default to all`() {
        assertEquals(Category.all.toSet(), AnalyzeUiState().categories)
    }
}
