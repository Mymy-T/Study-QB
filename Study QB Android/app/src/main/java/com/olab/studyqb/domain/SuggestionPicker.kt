package com.olab.studyqb.domain

import com.olab.studyqb.data.network.qbreader.dto.FrequencyEntryDto

/**
 * Chooses which frequency-list answer the Analyze tab's "Suggest" button drops into the query
 * box.
 *
 * Mirrors the 2026-10-19 change to AnalyzeView.swift's `suggestTerm()`, which stopped always
 * handing back the single most-asked unowned answer: it now sorts the candidates by question
 * count, keeps the top few, and draws from them at random while never repeating the answer it
 * suggested last. Tapping Suggest twice therefore moves you somewhere new instead of sitting on
 * the same answerline.
 *
 * Sorting is explicit even though qbreader already returns the list descending by count — the
 * previous code silently depended on that, and the pool is small enough that re-sorting costs
 * nothing.
 *
 * One divergence from iOS, for termination rather than behavior: `suggestTerm()` re-draws in a
 * `while !uniqueSuggestion` loop until the result differs from the previous suggestion, which
 * spins forever if the pool holds several entries sharing one answer string. Excluding the
 * previous answer before drawing picks once and cannot hang.
 */
object SuggestionPicker {

    /** iOS: "One of the top 5, or less if there aren't that many viable elements". */
    const val POOL_SIZE = 5

    /**
     * @param lastSuggested the answer handed out by the previous call, avoided if there is any
     *   alternative. Pass an empty string when nothing has been suggested yet.
     * @param isOwned whether the user already has a card covering that answer — normally
     *   [CardMatching.cardExists] bound to the current card list.
     * @return the answer to suggest, or null if every candidate is already owned.
     */
    fun pick(
        items: List<FrequencyEntryDto>,
        lastSuggested: String,
        isOwned: (String) -> Boolean
    ): FrequencyEntryDto? {
        val pool = items
            .filterNot { isOwned(it.answer) }
            .sortedByDescending { it.count }
            .take(POOL_SIZE)

        if (pool.size <= 1) return pool.firstOrNull()

        // If excluding the previous suggestion empties the pool, repeat it rather than report
        // nothing — "no suggestions available" is a worse answer than the same one twice.
        return pool.filterNot { it.answer == lastSuggested }.ifEmpty { pool }.random()
    }
}
