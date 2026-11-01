package com.olab.studyqb.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.olab.studyqb.data.db.entities.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observeById(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CardEntity>

    /** "Due today": fsrs_due strictly before the caller-computed start-of-tomorrow instant. */
    @Query("SELECT * FROM cards WHERE fsrs_due < :startOfTomorrowEpochMillis")
    suspend fun getDueBefore(startOfTomorrowEpochMillis: Long): List<CardEntity>

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()

    @Query("UPDATE cards SET cachedTossup = NULL")
    suspend fun clearAllCachedTossups()
}
