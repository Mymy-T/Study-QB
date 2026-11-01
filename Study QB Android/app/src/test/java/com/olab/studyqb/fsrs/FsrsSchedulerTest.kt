package com.olab.studyqb.fsrs

import com.olab.studyqb.data.fsrs.FsrsCard
import com.olab.studyqb.data.fsrs.FsrsScheduler
import com.olab.studyqb.data.fsrs.FsrsState
import com.olab.studyqb.data.fsrs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * These are sanity/property tests (monotonicity, clamping, the Again-override), not golden
 * vectors from a verified reference FSRS implementation — no reference implementation was
 * available to cross-check against while porting from SwiftFSRS. If exact due-date parity
 * with the iOS app is ever required, replace/augment these with real reference vectors
 * (e.g. from py-fsrs or rs-fsrs) before trusting the formulas in FsrsScheduler.kt blindly.
 */
class FsrsSchedulerTest {

    private val now: Instant = Instant.parse("2026-08-07T12:00:00Z")

    @Test
    fun `again always forces due to now regardless of computed interval`() {
        val reviewed = FsrsCard(
            due = now.minus(3, ChronoUnit.DAYS),
            stability = 20.0,
            difficulty = 5.0,
            elapsedDays = 10,
            scheduledDays = 10,
            reps = 4,
            lapses = 0,
            state = FsrsState.REVIEW,
            lastReview = now.minus(10, ChronoUnit.DAYS)
        )

        val result = FsrsScheduler.scheduleReview(reviewed, Rating.AGAIN, now)

        assertEquals(now, result.due)
        assertEquals(reviewed.lapses + 1, result.lapses)
    }

    @Test
    fun `new card first review sets state to review and reps to 1`() {
        val card = FsrsCard.new(now)
        val result = FsrsScheduler.scheduleReview(card, Rating.GOOD, now)

        assertEquals(FsrsState.REVIEW, result.state)
        assertEquals(1, result.reps)
        assertEquals(0, result.lapses)
        assertTrue(result.due.isAfter(now) || result.due == now)
    }

    @Test
    fun `difficulty stays within 1 to 10 across many reviews`() {
        var card = FsrsCard.new(now)
        var t = now
        val ratings = listOf(Rating.AGAIN, Rating.HARD, Rating.GOOD, Rating.EASY, Rating.GOOD, Rating.AGAIN, Rating.EASY)
        for (rating in ratings) {
            card = FsrsScheduler.scheduleReview(card, rating, t)
            assertTrue("difficulty ${card.difficulty} out of range", card.difficulty in 1.0..10.0)
            t = t.plus(1, ChronoUnit.DAYS)
        }
    }

    @Test
    fun `stability stays positive across many reviews`() {
        var card = FsrsCard.new(now)
        var t = now
        val ratings = listOf(Rating.GOOD, Rating.GOOD, Rating.AGAIN, Rating.HARD, Rating.EASY, Rating.GOOD)
        for (rating in ratings) {
            card = FsrsScheduler.scheduleReview(card, rating, t)
            assertTrue("stability ${card.stability} not positive", card.stability > 0.0)
            t = t.plus(card.scheduledDays.coerceAtLeast(1), ChronoUnit.DAYS)
        }
    }

    @Test
    fun `easy interval is never shorter than good, good never shorter than hard, from same starting state`() {
        val base = FsrsCard(
            due = now,
            stability = 10.0,
            difficulty = 5.0,
            elapsedDays = 5,
            scheduledDays = 5,
            reps = 3,
            lapses = 0,
            state = FsrsState.REVIEW,
            lastReview = now.minus(5, ChronoUnit.DAYS)
        )

        val hard = FsrsScheduler.scheduleReview(base, Rating.HARD, now)
        val good = FsrsScheduler.scheduleReview(base, Rating.GOOD, now)
        val easy = FsrsScheduler.scheduleReview(base, Rating.EASY, now)

        assertTrue(good.scheduledDays >= hard.scheduledDays)
        assertTrue(easy.scheduledDays >= good.scheduledDays)
    }

    @Test
    fun `initial stability and difficulty match the w vector for a brand new card`() {
        val card = FsrsCard.new(now)
        val result = FsrsScheduler.scheduleReview(card, Rating.GOOD, now)

        // S0(Good) = w[2]; sanity check it's a small positive number of days of stability.
        assertTrue(result.stability > 0.0)
        assertEquals(FsrsScheduler.initialStability(Rating.GOOD), result.stability, 1e-9)
        assertEquals(FsrsScheduler.initialDifficulty(Rating.GOOD), result.difficulty, 1e-9)
    }

    @Test
    fun `lapse stability never exceeds pre-lapse stability`() {
        val reviewed = FsrsCard(
            due = now,
            stability = 15.0,
            difficulty = 6.0,
            elapsedDays = 8,
            scheduledDays = 8,
            reps = 5,
            lapses = 0,
            state = FsrsState.REVIEW,
            lastReview = now.minus(8, ChronoUnit.DAYS)
        )

        val result = FsrsScheduler.scheduleReview(reviewed, Rating.AGAIN, now)

        assertTrue(result.stability <= reviewed.stability)
    }
}
