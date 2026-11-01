package com.olab.studyqb.data.fsrs

import java.time.Instant

enum class FsrsState {
    NEW, LEARNING, REVIEW, RELEARNING
}

/** Mirrors SwiftFSRS's `Rating`: quality of recall the user reports for a review. */
enum class Rating(val value: Int) {
    AGAIN(1), HARD(2), GOOD(3), EASY(4)
}

/**
 * FSRS v5 scheduling state for a single card. Mirrors the fields of SwiftFSRS's `Card`
 * struct (stored as an embedded/flattened set of columns on the Room `CardEntity`).
 */
data class FsrsCard(
    val due: Instant,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Long,
    val scheduledDays: Long,
    val reps: Int,
    val lapses: Int,
    val state: FsrsState,
    val lastReview: Instant?
) {
    companion object {
        /** A brand-new, never-reviewed card: due immediately, so it shows up in "due today". */
        fun new(now: Instant = Instant.now()): FsrsCard = FsrsCard(
            due = now,
            stability = 0.0,
            difficulty = 0.0,
            elapsedDays = 0,
            scheduledDays = 0,
            reps = 0,
            lapses = 0,
            state = FsrsState.NEW,
            lastReview = null
        )
    }
}
