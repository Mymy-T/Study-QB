package com.olab.studyqb.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.olab.studyqb.data.db.entities.BuzzGameStatEntity

data class LifetimeTotals(
    val totalPoints: Int?,
    val totalCorrect: Int?,
    val totalIncorrect: Int?
)

@Dao
interface BuzzGameStatDao {

    @Query("SELECT * FROM buzz_game_stats ORDER BY date DESC")
    fun observeAllNewestFirst(): Flow<List<BuzzGameStatEntity>>

    @Query(
        "SELECT SUM(points) AS totalPoints, SUM(numberCorrect) AS totalCorrect, " +
            "SUM(numberIncorrect) AS totalIncorrect FROM buzz_game_stats"
    )
    fun observeLifetimeTotals(): Flow<LifetimeTotals>

    @Insert
    suspend fun insert(stat: BuzzGameStatEntity): Long
}
