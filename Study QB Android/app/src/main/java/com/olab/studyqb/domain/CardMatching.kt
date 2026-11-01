package com.olab.studyqb.domain

import com.olab.studyqb.data.db.entities.CardEntity

/**
 * Replicates DataModel.swift's `cardExists(forAnswer:in:)`: case-insensitive dedup check used
 * by the Analyze view's "suggest a term" feature and the frequency list's "hide cards I
 * already have" toggle. Answers under 3 characters require an exact match (to avoid, e.g.,
 * "Ra" matching every card containing those two letters); longer answers match if either
 * string contains the other.
 */
object CardMatching {

    fun cardExists(answer: String, cards: List<CardEntity>): Boolean {
        val trimmed = answer.trim()
        if (trimmed.length < 3) {
            return cards.any { it.answer.trim().equals(trimmed, ignoreCase = true) }
        }
        return cards.any { card ->
            val cardAnswer = card.answer.trim()
            if (cardAnswer.length < 3) {
                cardAnswer.equals(trimmed, ignoreCase = true)
            } else {
                trimmed.contains(cardAnswer, ignoreCase = true) || cardAnswer.contains(trimmed, ignoreCase = true)
            }
        }
    }
}
