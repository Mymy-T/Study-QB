package com.olab.studyqb.data.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.olab.studyqb.data.fsrs.FsrsCard
import com.olab.studyqb.data.fsrs.FsrsState
import com.olab.studyqb.domain.Category
import java.time.Instant

/**
 * Flattened FSRS scheduling state, embedded into [CardEntity] with an "fsrs_" column prefix.
 * Mirrors iOS's embedded `FSRSCard` struct on `Card` (see DataModel.swift).
 */
data class FsrsColumns(
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
    fun toFsrsCard(): FsrsCard = FsrsCard(due, stability, difficulty, elapsedDays, scheduledDays, reps, lapses, state, lastReview)

    companion object {
        fun from(card: FsrsCard): FsrsColumns = FsrsColumns(
            due = card.due,
            stability = card.stability,
            difficulty = card.difficulty,
            elapsedDays = card.elapsedDays,
            scheduledDays = card.scheduledDays,
            reps = card.reps,
            lapses = card.lapses,
            state = card.state,
            lastReview = card.lastReview
        )

        fun new(now: Instant = Instant.now()): FsrsColumns = from(FsrsCard.new(now))
    }
}

/**
 * Room equivalent of iOS's `Card` SwiftData model (DataModel.swift, `CardSchemaV4.Card`).
 * No foreign keys to [CardStatEntity]/[BuzzGameStatEntity] — matching iOS, those are
 * correlated by (answer, category) string equality rather than a relational key, so stats
 * intentionally survive card deletion.
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateReviewed: Instant,
    val answer: String,
    /** Markdown-ish text; `**bold**` spans are rendered via MarkdownBoldText in the UI. */
    val details: String,
    val category: Category,
    /** Newline-delimited clue lines, easiest-first (per the card editor's own instructions). */
    val personalNotes: String,
    /** "r,g,b,a" snapshot of the card's display color; does not auto-follow category after creation. */
    val rgba: String,
    val markedForReview: Boolean = false,
    val cachedTossup: String? = null,
    @Embedded(prefix = "fsrs_") val fsrs: FsrsColumns
)
