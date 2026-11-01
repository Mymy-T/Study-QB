package com.olab.studyqb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatchTest {

    @Test
    fun `exact match`() {
        assertTrue(FuzzyMatch.isMatch("Napoleon Bonaparte", "Napoleon Bonaparte"))
    }

    @Test
    fun `leading article is stripped`() {
        assertTrue(FuzzyMatch.isMatch("Napoleon Bonaparte", "the Napoleon Bonaparte"))
        assertTrue(FuzzyMatch.isMatch("Great Gatsby", "The Great Gatsby"))
    }

    @Test
    fun `containment match either direction`() {
        assertTrue(FuzzyMatch.isMatch("Gatsby", "The Great Gatsby"))
        assertTrue(FuzzyMatch.isMatch("The Great Gatsby", "Gatsby"))
    }

    /**
     * The window iOS added on 2026-10-19 (`abs(t.count - c.count) <= 10`). Before it, a short
     * query matched any answerline that merely contained it, however long — which in a 1010-line
     * dataset of slugs means the wrong analysis comes back. Normalized, "odyssey" (7) against
     * "odysseyandtheiliad" (18) is a gap of 11, and the levenshtein fallback (threshold 4,
     * distance 11) does not rescue it.
     */
    @Test
    fun `containment stops once the length gap exceeds ten`() {
        assertFalse(FuzzyMatch.isMatch("Odyssey", "The Odyssey and the Iliad"))
        // Normalized "beethoven" (9) vs "beethovensymphonyno5" (20): gap 11.
        assertFalse(FuzzyMatch.isMatch("Beethoven", "Beethoven Symphony No 5"))
    }

    @Test
    fun `containment still holds at a gap of exactly ten`() {
        // "beethoven" (9) vs "beethovensymphonies" (19): gap 10, the last accepted value.
        assertTrue(FuzzyMatch.isMatch("Beethoven", "Beethoven Symphonies"))
        // Well inside the window at a gap of 8.
        assertTrue(FuzzyMatch.isMatch("Beethoven", "Beethoven Symphony"))
    }

    @Test
    fun `small typo within levenshtein threshold matches`() {
        assertTrue(FuzzyMatch.isMatch("Napoleon Bonapart", "Napoleon Bonaparte"))
    }

    @Test
    fun `clearly wrong guess does not match`() {
        assertFalse(FuzzyMatch.isMatch("Julius Caesar", "Napoleon Bonaparte"))
    }

    @Test
    fun `empty guess never matches`() {
        assertFalse(FuzzyMatch.isMatch("", "Napoleon Bonaparte"))
    }

    /**
     * The guard iOS added with the cached dataset: a very short query is a substring of a huge
     * share of the 1010 answer lines, so containment alone must not be enough to match.
     */
    @Test
    fun `short containment does not match on substring alone`() {
        assertFalse(FuzzyMatch.isMatch("a", "Madame Bovary"))
        assertFalse(FuzzyMatch.isMatch("man", "Invisible Man"))
        assertFalse(FuzzyMatch.isMatch("war", "The Winter War"))
    }

    @Test
    fun `short strings can still match by edit distance`() {
        assertTrue(FuzzyMatch.isMatch("Rome", "Rome"))
        assertTrue(FuzzyMatch.isMatch("Mars", "Marz"))
    }

    /**
     * Guards our divergence from Functions.swift, whose `_normalizeQuery` strips spaces before
     * testing the "the "/"a "/"an " prefixes and so can never strip an article. Under iOS's
     * ordering this pair fails: levenshtein("odyssey", "theodyssey") is 3, threshold is 2.
     */
    @Test
    fun `article stripping survives the normalize reorder`() {
        assertTrue(FuzzyMatch.isMatch("Odyssey", "The Odyssey"))
        assertEquals("odyssey", FuzzyMatch.normalize("The Odyssey"))
        assertEquals("iliad", FuzzyMatch.normalize("An Iliad"))
    }

    /** Dataset answers arrive as slugs; underscores must not defeat matching. */
    @Test
    fun `dataset slug form matches a typed answer`() {
        assertTrue(FuzzyMatch.isMatch("Madame Bovary", "madame_bovary".replace('_', ' ')))
        assertTrue(FuzzyMatch.isMatch("prisoner's dilemma", "prisoner's_dilemma".replace('_', ' ')))
    }
}
