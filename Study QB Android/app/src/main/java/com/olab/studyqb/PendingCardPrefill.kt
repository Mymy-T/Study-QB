package com.olab.studyqb

import com.olab.studyqb.domain.Category

/**
 * Ephemeral in-memory handoff for Analyze's "Save" button, which needs to open the Card
 * Editor pre-filled with an arbitrary-length answer/details string — too unwieldy to pass
 * safely through a nav route argument, so it's stashed here just before navigating and
 * consumed once by the Card Editor.
 *
 * [Prefill.category] rides along for the same reason rather than as a route argument: it only
 * ever travels with the answer and details, and keeping all three together means there is one
 * thing to set and one thing to consume.
 */
object PendingCardPrefill {

    data class Prefill(
        val answer: String? = null,
        val details: String? = null,
        /** Null when Analyze had no unambiguous category to offer; the editor then defaults. */
        val category: Category? = null
    )

    private var pending = Prefill()

    fun set(answer: String, details: String, category: Category? = null) {
        pending = Prefill(answer, details, category)
    }

    fun take(): Prefill {
        val result = pending
        pending = Prefill()
        return result
    }
}
