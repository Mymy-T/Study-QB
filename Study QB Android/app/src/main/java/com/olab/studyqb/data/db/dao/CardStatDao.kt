package com.olab.studyqb.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.olab.studyqb.data.db.entities.CardStatEntity
import com.olab.studyqb.domain.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CardStatDao {

    @Query("SELECT * FROM card_stats ORDER BY answer ASC")
    fun observeAllSortedByAnswer(): Flow<List<CardStatEntity>>

    @Query("SELECT * FROM card_stats WHERE answer = :answer AND category = :category LIMIT 1")
    suspend fun findByAnswerAndCategory(answer: String, category: Category): CardStatEntity?

    @Insert
    suspend fun insert(stat: CardStatEntity): Long

    @Update
    suspend fun update(stat: CardStatEntity)

    /**
     * Upserts a Buzz In attempt by (answer, category) string equality, matching iOS's
     * `recordCardAttempt(correct:)` in BuzzInView.swift — not a foreign key to a Card, so
     * this survives the source card being deleted/recreated.
     */
    @Transaction
    suspend fun recordAttempt(answer: String, category: Category, correct: Boolean, buzzPositionPercent: Double) {
        val existing = findByAnswerAndCategory(answer, category)
        if (existing == null) {
            insert(
                CardStatEntity(
                    answer = answer,
                    category = category,
                    timesSeen = 1,
                    timesCorrect = if (correct) 1 else 0,
                    totalBuzzPositionPercent = buzzPositionPercent
                )
            )
        } else {
            update(
                existing.copy(
                    timesSeen = existing.timesSeen + 1,
                    timesCorrect = existing.timesCorrect + if (correct) 1 else 0,
                    totalBuzzPositionPercent = existing.totalBuzzPositionPercent + buzzPositionPercent
                )
            )
        }
    }
}
