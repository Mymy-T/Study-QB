package com.olab.studyqb.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.olab.studyqb.domain.Category

/**
 * Per-(answer, category) cumulative Buzz In performance stats. Mirrors iOS's `CardStat`
 * SwiftData model — matched by string equality, not a foreign key to [CardEntity], so a
 * card's history survives it being deleted/recreated.
 */
@Entity(
    tableName = "card_stats",
    indices = [Index(value = ["answer", "category"], unique = true)]
)
data class CardStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val answer: String,
    val category: Category,
    val timesSeen: Int = 0,
    val timesCorrect: Int = 0,
    val totalBuzzPositionPercent: Double = 0.0
) {
    /** Matches iOS's computed `averageBuzzPositionPercent` — not a stored column. */
    val averageBuzzPositionPercent: Double
        get() = if (timesSeen > 0) totalBuzzPositionPercent / timesSeen else 0.0
}
