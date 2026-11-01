package com.olab.studyqb.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olab.studyqb.domain.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardStatDaoTest {
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

    @Test
    fun recordAttempt_insertsNewRowOnFirstAttempt() = runBlocking {
        db.cardStatDao().recordAttempt("Napoleon Bonaparte", Category.HISTORY, correct = true, buzzPositionPercent = 40.0)

        val stat = db.cardStatDao().findByAnswerAndCategory("Napoleon Bonaparte", Category.HISTORY)!!
        assertEquals(1, stat.timesSeen)
        assertEquals(1, stat.timesCorrect)
        assertEquals(40.0, stat.totalBuzzPositionPercent, 0.0001)
    }

    @Test
    fun recordAttempt_updatesExistingRowRatherThanDuplicating() = runBlocking {
        db.cardStatDao().recordAttempt("Napoleon Bonaparte", Category.HISTORY, correct = true, buzzPositionPercent = 40.0)
        db.cardStatDao().recordAttempt("Napoleon Bonaparte", Category.HISTORY, correct = false, buzzPositionPercent = 80.0)

        val stat = db.cardStatDao().findByAnswerAndCategory("Napoleon Bonaparte", Category.HISTORY)!!
        assertEquals(2, stat.timesSeen)
        assertEquals(1, stat.timesCorrect)
        assertEquals(120.0, stat.totalBuzzPositionPercent, 0.0001)
        assertEquals(60.0, stat.averageBuzzPositionPercent, 0.0001)

        // Only one row should exist for this (answer, category) pair.
        assertEquals(1, db.cardStatDao().observeAllSortedByAnswer().first().size)
    }
}
