package com.olab.studyqb.ui.cards

enum class SortParameter(val label: String) {
    REVIEW_DATE_REVERSE("Most Recently Reviewed"),
    REVIEW_DATE_FORWARD("Oldest Reviewed"),
    TITLE_ALPHABETICAL_FORWARD("Alphabetical, by Title (A-Z)"),
    TITLE_ALPHABETICAL_REVERSE("Alphabetical, by Title (Z-A)")
}
