package com.olab.studyqb.domain

/**
 * Per-category fallback sentence starters used to build a deterministic, rule-based tossup
 * when Groq is unreachable (mirrors iOS's `ruleBasedTossup` fallback in BuzzInView.swift).
 */
object CategorySentenceStarters {
    val starters: Map<Category, String> = mapOf(
        Category.LITERATURE to "This literary work",
        Category.HISTORY to "This historical figure or event",
        Category.SCIENCE to "This scientific concept",
        Category.FINE_ARTS to "This work of art",
        Category.RELIGION to "This religious topic",
        Category.MYTHOLOGY to "This mythological figure or story",
        Category.PHILOSOPHY to "This philosophical concept",
        Category.SOCIAL_SCIENCE to "This social science concept",
        Category.CURRENT_EVENTS to "This current event or figure",
        Category.GEOGRAPHY to "This place"
    )

    fun starterFor(category: Category): String = starters[category] ?: "This topic"

    /**
     * Deterministic tossup built from a card's clue lines with no LLM call, matching iOS's
     * offline fallback: clues reversed (hardest-first) and prefixed by a fixed per-category
     * sentence starter, joined into a single paragraph.
     */
    fun buildFallbackTossup(category: Category, personalNotes: String): String {
        val starter = starterFor(category)
        val clues = personalNotes
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .asReversed()
        if (clues.isEmpty()) return "$starter has no recorded clues."
        return clues.joinToString(separator = ". ") { clue -> "$starter $clue" } + "."
    }
}
