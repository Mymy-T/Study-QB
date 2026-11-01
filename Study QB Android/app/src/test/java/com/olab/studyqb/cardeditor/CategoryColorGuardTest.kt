package com.olab.studyqb.cardeditor

import com.olab.studyqb.domain.Category
import com.olab.studyqb.domain.ColorPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-logic re-implementation of CardEditorViewModel.setCategory's decision rule, exercised
 * directly (without needing Room/Android) to lock in the exact iOS quirk it replicates: see
 * CardEditorViewModel.setCategory's doc comment for the full explanation.
 */
class CategoryColorGuardTest {

    private data class State(
        val isNewCard: Boolean,
        val originalLoadedCategory: Category,
        val firstCategoryChangeConsumed: Boolean,
        val rgba: List<Double>
    )

    private fun setCategory(state: State, newCategory: Category): State {
        val shouldRecolor = when {
            state.isNewCard -> true
            !state.firstCategoryChangeConsumed -> state.originalLoadedCategory == Category.LITERATURE
            else -> true
        }
        return state.copy(
            rgba = if (shouldRecolor) ColorPalette.colorToRgba(ColorPalette.colorForCategory(newCategory)) else state.rgba,
            firstCategoryChangeConsumed = true
        )
    }

    @Test
    fun `new card always recolors on every category change`() {
        var state = State(isNewCard = true, originalLoadedCategory = Category.LITERATURE, firstCategoryChangeConsumed = false, rgba = listOf(9.0, 9.0, 9.0, 9.0))
        state = setCategory(state, Category.HISTORY)
        assertEquals(ColorPalette.colorToRgba(ColorPalette.colorForCategory(Category.HISTORY)), state.rgba)
    }

    @Test
    fun `existing non-literature card suppresses the first category change color update`() {
        val customColor = listOf(0.1, 0.2, 0.3, 0.4)
        var state = State(isNewCard = false, originalLoadedCategory = Category.HISTORY, firstCategoryChangeConsumed = false, rgba = customColor)

        state = setCategory(state, Category.SCIENCE) // first change: suppressed
        assertEquals(customColor, state.rgba)

        state = setCategory(state, Category.GEOGRAPHY) // second change: applies normally
        assertNotEquals(customColor, state.rgba)
        assertEquals(ColorPalette.colorToRgba(ColorPalette.colorForCategory(Category.GEOGRAPHY)), state.rgba)
    }

    @Test
    fun `existing literature card recolors on the first category change`() {
        val customColor = listOf(0.1, 0.2, 0.3, 0.4)
        var state = State(isNewCard = false, originalLoadedCategory = Category.LITERATURE, firstCategoryChangeConsumed = false, rgba = customColor)

        state = setCategory(state, Category.SCIENCE) // first change: NOT suppressed for a Literature-loaded card

        assertEquals(ColorPalette.colorToRgba(ColorPalette.colorForCategory(Category.SCIENCE)), state.rgba)
    }
}
