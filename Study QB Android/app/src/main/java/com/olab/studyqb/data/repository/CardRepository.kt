package com.olab.studyqb.data.repository

import com.olab.studyqb.data.db.dao.CardDao
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.db.entities.FsrsColumns
import com.olab.studyqb.data.fsrs.FsrsCard
import com.olab.studyqb.domain.Category
import com.olab.studyqb.domain.ColorPalette
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CardRepository(private val cardDao: CardDao) {

    fun observeAll(): Flow<List<CardEntity>> = cardDao.observeAll()

    fun observeById(id: Long): Flow<CardEntity?> = cardDao.observeById(id)

    suspend fun getById(id: Long): CardEntity? = cardDao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<CardEntity> = cardDao.getByIds(ids)

    /** Cards whose FSRS due date falls on or before today (i.e. due before the start of tomorrow). */
    suspend fun getDueToday(zoneId: ZoneId = ZoneId.systemDefault()): List<CardEntity> {
        val startOfTomorrow = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant()
        return cardDao.getDueBefore(startOfTomorrow.toEpochMilli())
    }

    suspend fun createCard(
        answer: String,
        details: String,
        category: Category,
        personalNotes: String,
        rgba: List<Double>,
        markedForReview: Boolean,
        dateReviewed: Instant = Instant.now()
    ): Long {
        val entity = CardEntity(
            dateReviewed = dateReviewed,
            answer = answer,
            details = details,
            category = category,
            personalNotes = personalNotes,
            rgba = ColorPalette.rgbaToString(rgba),
            markedForReview = markedForReview,
            cachedTossup = null,
            fsrs = FsrsColumns.new(dateReviewed)
        )
        return cardDao.insert(entity)
    }

    suspend fun updateCard(card: CardEntity) = cardDao.update(card)

    suspend fun deleteCard(card: CardEntity) = cardDao.delete(card)

    suspend fun deleteAll() = cardDao.deleteAll()

    suspend fun clearAllCachedTossups() = cardDao.clearAllCachedTossups()

    suspend fun updateCachedTossup(card: CardEntity, tossup: String) {
        cardDao.update(card.copy(cachedTossup = tossup))
    }

    suspend fun updateFsrsState(card: CardEntity, fsrsCard: FsrsCard) {
        cardDao.update(card.copy(fsrs = FsrsColumns.from(fsrsCard)))
    }

    suspend fun toggleMarkedForReview(card: CardEntity, marked: Boolean) {
        cardDao.update(card.copy(markedForReview = marked))
    }

    suspend fun markReviewedNow(card: CardEntity, now: Instant = Instant.now()) {
        cardDao.update(card.copy(dateReviewed = now))
    }
}
