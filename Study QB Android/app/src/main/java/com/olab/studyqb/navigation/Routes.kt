package com.olab.studyqb.navigation

object Routes {
    const val ANALYZE = "analyze"
    const val CARDS = "cards"
    const val CARD_SELECTION = "cardSelection"
    const val STATS = "stats"
    const val CARD_PERFORMANCE = "cardPerformance"
    const val SETTINGS = "settings"

    const val CARD_EDITOR_PATTERN = "cardEditor?cardId={cardId}&usePrefill={usePrefill}"
    fun cardEditor(cardId: Long? = null, usePrefill: Boolean = false) =
        "cardEditor?cardId=${cardId ?: -1}&usePrefill=$usePrefill"

    /** [resetKey] changes on every bottom-nav tap into Review, forcing a fresh backstack entry. */
    const val REVIEW_PATTERN = "review/{resetKey}"
    fun review(resetKey: Int) = "review/$resetKey"

    const val CLASSIC_PRACTICE = "classicPractice"
    const val BUZZ_IN = "buzzIn"
}
