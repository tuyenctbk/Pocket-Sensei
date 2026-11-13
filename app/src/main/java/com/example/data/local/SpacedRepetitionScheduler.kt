package com.example.data.local

import java.util.concurrent.TimeUnit

data class ReviewSchedule(
    val nextMasteryLevel: Int,
    val nextIntervalDays: Int,
    val nextReviewAtMs: Long
)

/**
 * Pure Leitner / SM-2 inspired spaced-repetition scheduler.
 * Expands intervals cleanly on recall (1d -> 3d -> 7d -> 14d -> 30d) and resets
 * to immediate review (1d interval, due now) when a learner flags the word as needing work.
 */
object SpacedRepetitionScheduler {

    fun computeNextSchedule(
        currentMastery: Int,
        currentIntervalDays: Int,
        remembered: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): ReviewSchedule {
        return if (remembered) {
            val nextLevel = (currentMastery + 1).coerceIn(1, 5)
            val nextInterval = when {
                currentIntervalDays <= 0 -> 1
                currentIntervalDays == 1 -> 3
                currentIntervalDays <= 3 -> 7
                currentIntervalDays <= 7 -> 14
                currentIntervalDays <= 14 -> 30
                else -> (currentIntervalDays * 2).coerceAtMost(60)
            }
            val nextDue = nowMs + TimeUnit.DAYS.toMillis(nextInterval.toLong())
            ReviewSchedule(
                nextMasteryLevel = nextLevel,
                nextIntervalDays = nextInterval,
                nextReviewAtMs = nextDue
            )
        } else {
            val nextLevel = (currentMastery - 1).coerceIn(1, 5)
            ReviewSchedule(
                nextMasteryLevel = nextLevel,
                nextIntervalDays = 1,
                nextReviewAtMs = nowMs
            )
        }
    }

    fun computeScheduleForScore(
        currentMastery: Int,
        currentIntervalDays: Int,
        pronunciationScore: Int,
        nowMs: Long = System.currentTimeMillis()
    ): ReviewSchedule {
        return when {
            pronunciationScore >= 85 -> computeNextSchedule(
                currentMastery = currentMastery,
                currentIntervalDays = currentIntervalDays,
                remembered = true,
                nowMs = nowMs
            )
            pronunciationScore >= 70 -> ReviewSchedule(
                nextMasteryLevel = maxOf(currentMastery, 2).coerceIn(1, 5),
                nextIntervalDays = 1,
                nextReviewAtMs = nowMs + TimeUnit.DAYS.toMillis(1)
            )
            else -> ReviewSchedule(
                nextMasteryLevel = currentMastery.coerceIn(1, 5),
                nextIntervalDays = 1,
                nextReviewAtMs = nowMs
            )
        }
    }

    fun isDueForReview(
        word: VocabularyWord,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean = word.nextReviewAt <= nowMs
}
