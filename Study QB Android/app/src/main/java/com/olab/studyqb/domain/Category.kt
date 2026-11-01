package com.olab.studyqb.domain

/**
 * The 10 quizbowl subject categories, in the exact order/labels used by the original
 * iOS app (and therefore by qbreader.org's category codes, which are positional).
 */
enum class Category(val displayName: String) {
    LITERATURE("Literature"),
    HISTORY("History"),
    SCIENCE("Science"),
    FINE_ARTS("Fine Arts"),
    RELIGION("Religion"),
    MYTHOLOGY("Mythology"),
    PHILOSOPHY("Philosophy"),
    SOCIAL_SCIENCE("Social Science"),
    CURRENT_EVENTS("Current Events"),
    GEOGRAPHY("Geography");

    companion object {
        val all: List<Category> = entries.toList()
    }
}
