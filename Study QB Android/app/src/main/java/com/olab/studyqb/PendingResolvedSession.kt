package com.olab.studyqb

import com.olab.studyqb.ui.review.ResolvedSession

/** Same ephemeral in-memory handoff pattern as [PendingCardPrefill], for passing a resolved
 * practice session's card-ID list from Practice Setup to the Classic/Buzz In screens without
 * needing to encode a list of IDs into a nav route argument. */
object PendingResolvedSession {
    private var session: ResolvedSession? = null

    fun set(value: ResolvedSession) {
        session = value
    }

    fun take(): ResolvedSession? {
        val result = session
        session = null
        return result
    }
}
