package com.olab.studyqb.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** One row per saved Buzz In session. Mirrors iOS's `BuzzGameStat` SwiftData model. */
@Entity(tableName = "buzz_game_stats")
data class BuzzGameStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Instant,
    val points: Int,
    val numberCorrect: Int,
    val numberIncorrect: Int
)
