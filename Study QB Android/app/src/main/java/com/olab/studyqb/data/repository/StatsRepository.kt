package com.olab.studyqb.data.repository

import com.olab.studyqb.data.db.dao.BuzzGameStatDao
import com.olab.studyqb.data.db.dao.CardStatDao
import com.olab.studyqb.data.db.dao.LifetimeTotals
import com.olab.studyqb.data.db.entities.BuzzGameStatEntity
import com.olab.studyqb.data.db.entities.CardStatEntity
import com.olab.studyqb.domain.Category
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class StatsRepository(
    private val buzzGameStatDao: BuzzGameStatDao,
    private val cardStatDao: CardStatDao
) {
    fun observeGamesNewestFirst(): Flow<List<BuzzGameStatEntity>> = buzzGameStatDao.observeAllNewestFirst()

    fun observeLifetimeTotals(): Flow<LifetimeTotals> = buzzGameStatDao.observeLifetimeTotals()

    fun observeCardStatsSortedByAnswer(): Flow<List<CardStatEntity>> = cardStatDao.observeAllSortedByAnswer()

    suspend fun saveGame(points: Int, numberCorrect: Int, numberIncorrect: Int, date: Instant = Instant.now()) {
        buzzGameStatDao.insert(BuzzGameStatEntity(date = date, points = points, numberCorrect = numberCorrect, numberIncorrect = numberIncorrect))
    }

    suspend fun recordCardAttempt(answer: String, category: Category, correct: Boolean, buzzPositionPercent: Double) {
        cardStatDao.recordAttempt(answer, category, correct, buzzPositionPercent)
    }
}
