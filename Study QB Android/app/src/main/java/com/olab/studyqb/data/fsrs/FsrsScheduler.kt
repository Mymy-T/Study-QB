package com.olab.studyqb.data.fsrs

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * A from-scratch Kotlin implementation of the FSRS v5 "long-term scheduler" (no short-term
 * same-day learning steps), matching how the iOS app invokes SwiftFSRS's `LongTermScheduler`
 * with `algorithm: .v5`. Pure functions, no Android dependencies, so this is unit-testable
 * on the plain JVM.
 *
 * The one behavior that must be preserved exactly regardless of the formula details below:
 * a review rated [Rating.AGAIN] always forces the card's `due` date to `now`, applied as a
 * final override after the FSRS math (see [scheduleReview]).
 */
object FsrsScheduler {

    fun retrievability(elapsedDays: Double, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + FsrsParameters.FACTOR * elapsedDays / stability).pow(FsrsParameters.DECAY)
    }

    fun initialDifficulty(rating: Rating): Double {
        val w = FsrsParameters.w
        val d = w[4] - exp(w[5] * (rating.value - 1)) + 1.0
        return d.coerceIn(1.0, 10.0)
    }

    fun initialStability(rating: Rating): Double = FsrsParameters.w[rating.value - 1]

    fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val w = FsrsParameters.w
        val deltaD = -w[6] * (rating.value - 3)
        val dPrime = difficulty + deltaD * (10.0 - difficulty) / 9.0
        val easyD0 = initialDifficulty(Rating.EASY)
        val dDoublePrime = w[7] * easyD0 + (1.0 - w[7]) * dPrime
        return dDoublePrime.coerceIn(1.0, 10.0)
    }

    fun nextStabilityOnSuccess(stability: Double, difficulty: Double, retrievability: Double, rating: Rating): Double {
        val w = FsrsParameters.w
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
        return stability * (
            1.0 + exp(w[8]) *
                (11.0 - difficulty) *
                stability.pow(-w[9]) *
                (exp((1.0 - retrievability) * w[10]) - 1.0) *
                hardPenalty *
                easyBonus
        )
    }

    fun nextStabilityOnLapse(stability: Double, difficulty: Double, retrievability: Double): Double {
        val w = FsrsParameters.w
        val newStability = w[11] *
            difficulty.pow(-w[12]) *
            ((stability + 1.0).pow(w[13]) - 1.0) *
            exp((1.0 - retrievability) * w[14])
        return minOf(newStability, stability)
    }

    fun intervalDays(stability: Double, desiredRetention: Double = FsrsParameters.REQUEST_RETENTION): Long {
        val raw = (stability / FsrsParameters.FACTOR) * (desiredRetention.pow(1.0 / FsrsParameters.DECAY) - 1.0)
        return raw.roundToLong().coerceIn(1, FsrsParameters.MAXIMUM_INTERVAL_DAYS)
    }

    /**
     * Runs the FSRS v5 algorithm for a single review and returns the card's post-review
     * state. Does not persist anything — callers write the result back themselves.
     */
    fun scheduleReview(card: FsrsCard, rating: Rating, now: Instant = Instant.now()): FsrsCard {
        val elapsedDays = card.lastReview?.let {
            Duration.between(it, now).toMillis() / 86_400_000.0
        } ?: 0.0

        val scheduled = if (card.state == FsrsState.NEW) {
            val difficulty = initialDifficulty(rating)
            val stability = initialStability(rating)
            val days = intervalDays(stability)
            card.copy(
                difficulty = difficulty,
                stability = stability,
                state = FsrsState.REVIEW,
                reps = 1,
                lapses = 0,
                elapsedDays = 0,
                scheduledDays = days,
                lastReview = now,
                due = now.plus(days, ChronoUnit.DAYS)
            )
        } else {
            val r = retrievability(elapsedDays, card.stability)
            val newDifficulty = nextDifficulty(card.difficulty, rating)
            val newStability = if (rating == Rating.AGAIN) {
                nextStabilityOnLapse(card.stability, card.difficulty, r)
            } else {
                nextStabilityOnSuccess(card.stability, card.difficulty, r, rating)
            }
            val days = intervalDays(newStability)
            card.copy(
                difficulty = newDifficulty,
                stability = newStability,
                reps = if (rating != Rating.AGAIN) card.reps + 1 else card.reps,
                lapses = if (rating == Rating.AGAIN) card.lapses + 1 else card.lapses,
                elapsedDays = elapsedDays.roundToLong(),
                scheduledDays = days,
                lastReview = now,
                state = if (rating == Rating.AGAIN) FsrsState.RELEARNING else FsrsState.REVIEW,
                due = now.plus(days, ChronoUnit.DAYS)
            )
        }

        // *** Override, matching DataModel.swift's scheduleReview(): Again always means
        // *** "due right now", regardless of what the FSRS interval math computed.
        return if (rating == Rating.AGAIN) scheduled.copy(due = now) else scheduled
    }
}
