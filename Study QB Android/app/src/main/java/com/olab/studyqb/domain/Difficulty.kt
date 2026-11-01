package com.olab.studyqb.domain

/**
 * qbreader.org's difficulty codes (0-10) and their labels, plus the "general" quick-group
 * ranges used by the difficulty filter menu, matching the original iOS app exactly.
 */
object Difficulty {
    val labels: Map<Int, String> = mapOf(
        0 to "Pop Culture",
        1 to "Middle School",
        2 to "Easy High School",
        3 to "Regular High School",
        4 to "Hard High School",
        5 to "National High School",
        6 to "Easy College",
        7 to "Medium College",
        8 to "Regionals College",
        9 to "Nationals College",
        10 to "Open"
    )

    val all: List<Int> = (0..10).toList()

    val middleSchool: Set<Int> = setOf(1)
    val highSchool: Set<Int> = (2..5).toSet()
    val college: Set<Int> = (6..9).toSet()
}
