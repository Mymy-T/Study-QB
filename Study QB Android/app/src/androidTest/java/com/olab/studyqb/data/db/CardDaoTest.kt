package com.olab.studyqb.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.db.entities.FsrsColumns
import com.olab.studyqb.domain.Category
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class CardDaoTest {
    private lateinit var db: StudyQBDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, StudyQBDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun cardEntity(
        answer: String,
        category: Category = Category.LITERATURE,
        due: Instant = Instant.now(),
        dateReviewed: Instant = Instant.now()
    ) = CardEntity(
        dateReviewed = dateReviewed,
        answer = answer,
        details = "some details",
        category = category,
        personalNotes = "clue one\nclue two",
        rgba = "1.0,0.0,0.0,0.4",
        fsrs = FsrsColumns.new(due)
    )

    @Test
    fun insertAndReadBack_roundTripsAllFields() = runBlocking {
        val now = Instant.now()
        val id = db.cardDao().insert(cardEntity("Napoleon Bonaparte", Category.HISTORY, due = now, dateReviewed = now))
        val loaded = db.cardDao().getById(id)!!

        assertEquals("Napoleon Bonaparte", loaded.answer)
        assertEquals(Category.HISTORY, loaded.category)
        assertEquals("1.0,0.0,0.0,0.4", loaded.rgba)
        assertEquals(now.toEpochMilli(), loaded.fsrs.due.toEpochMilli())
        assertEquals(now.toEpochMilli(), loaded.dateReviewed.toEpochMilli())
    }

    @Test
    fun getDueOnOrBefore_onlyReturnsCardsDueByThatInstant() = runBlocking {
        val now = Instant.now()
        val yesterday = now.minus(1, ChronoUnit.DAYS)
        val tomorrow = now.plus(1, ChronoUnit.DAYS)

        db.cardDao().insert(cardEntity("due yesterday", due = yesterday))
        db.cardDao().insert(cardEntity("due today", due = now))
        db.cardDao().insert(cardEntity("due tomorrow", due = tomorrow))

        val due = db.cardDao().getDueOnOrBefore(now.toEpochMilli())

        assertEquals(2, due.size)
        assertTrue(due.any { it.answer == "due yesterday" })
        assertTrue(due.any { it.answer == "due today" })
        assertTrue(due.none { it.answer == "due tomorrow" })
    }

    @Test
    fun deleteAll_removesEveryCard() = runBlocking {
        db.cardDao().insert(cardEntity("a"))
        db.cardDao().insert(cardEntity("b"))

        db.cardDao().deleteAll()

        assertTrue(db.cardDao().getByIds(listOf(1L, 2L)).isEmpty())
    }

    @Test
    fun clearAllCachedTossups_setsNullOnAllCards() = runBlocking {
        val id = db.cardDao().insert(cardEntity("a"))
        db.cardDao().update(db.cardDao().getById(id)!!.copy(cachedTossup = "a cached tossup"))

        db.cardDao().clearAllCachedTossups()

        assertEquals(null, db.cardDao().getById(id)!!.cachedTossup)
    }
}
