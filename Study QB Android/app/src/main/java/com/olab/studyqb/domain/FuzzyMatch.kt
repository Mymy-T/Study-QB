package com.olab.studyqb.domain

import kotlin.math.abs

/**
 * Replicates Functions.swift's `fuzzyMatch`, which iOS promoted out of BuzzInView when the
 * cached-clue dataset started needing it too. Two callers now: the suggested
 * "Got it"/"Didn't get it" button in Buzz In (a hint — the user still taps one), and matching
 * an Analyze query against a bundled dataset answer.
 *
 * One deliberate divergence from iOS. `_normalizeQuery` there strips every non-alphanumeric
 * character *before* testing for the leading articles "the ", "a ", "an " — but those prefixes
 * contain spaces that no longer exist by then, so the article stripping cannot ever fire.
 * iOS's own comment ("Neglects specific answerlines such as 'The Invisible Man'...") shows the
 * stripping was meant to work. We strip first, then filter, so "Odyssey" still matches
 * "The Odyssey"; under iOS's current ordering that pair fails (levenshtein 3 > threshold 2).
 * The equivalent two-line reorder is still owed to Functions.swift.
 */
object FuzzyMatch {

    private val leadingArticles = listOf("the ", "an ", "a ")

    /** Minimum length both sides must exceed before a containment match counts. */
    private const val SUBSTRING_MIN_LENGTH = 4

    /**
     * How far apart the two lengths may be and still count as a containment match. iOS carries
     * a TODO on this number ("Tune this, or at least allow user to change the default loaded
     * card, or ensure that there's a match on qb reader") — it is a first guess, not a result.
     */
    private const val SUBSTRING_MAX_LENGTH_GAP = 10

    fun normalize(input: String): String {
        var s = input.lowercase().trim()
        for (article in leadingArticles) {
            if (s.startsWith(article)) {
                s = s.removePrefix(article)
                break
            }
        }
        return s.filter { it.isLetterOrDigit() }
    }

    fun isMatch(guess: String, correctAnswer: String): Boolean {
        val g = normalize(guess)
        val c = normalize(correctAnswer)
        if (g.isEmpty() || c.isEmpty()) return false
        // Length guards added by iOS alongside the dataset lookup. Without the minimum, a 1-2
        // character query is a substring of nearly every answer, so "a" would match the whole
        // dataset. Without the gap, a short query still matches a far longer answerline that
        // merely contains it, and the 1010-line dataset is full of those.
        if (c.contains(g) || g.contains(c)) {
            if (c.length > SUBSTRING_MIN_LENGTH &&
                g.length > SUBSTRING_MIN_LENGTH &&
                abs(g.length - c.length) <= SUBSTRING_MAX_LENGTH_GAP
            ) return true
        }
        val threshold = maxOf(1, c.length / 4)
        return levenshtein(g, c) <= threshold
    }

    fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
